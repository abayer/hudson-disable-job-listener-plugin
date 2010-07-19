package hudson.plugins.disablelistener;

import hudson.Extension;
import hudson.Util;
import hudson.XmlFile;

import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Saveable;
import hudson.model.TaskListener;
import hudson.model.User;
import hudson.model.listeners.SaveableListener;
import hudson.tasks.Mailer;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.logging.Level;

@Extension
public class DisableListener extends SaveableListener {
    private transient List<String> disabledProjects;

    private static final Logger LOGGER = Logger.getLogger(DisableListener.class.getName());
    
    public DisableListener() {
        disabledProjects = new ArrayList<String>();
        for (AbstractProject<?, ?> proj : Hudson.getInstance().getAllItems(AbstractProject.class)) {
            if (proj.isDisabled()) {
                disabledProjects.add(proj.getName());
            }
        }
    }

    @Override
    public void onChange(final Saveable o, final XmlFile file) {
        if (o instanceof AbstractProject) {
            AbstractProject<?, ?> p = (AbstractProject) o; 
            DisableJobProperty djp = p.getProperty(DisableJobProperty.class);

            if (djp!=null && djp.getNotify()) {
                String recipients = djp.getRecipients();
                
                // If the project is disabled but we didn't already have it in our list of
                // disabled projects...
                if (p.isDisabled() && !disabledProjects.contains(p.getName())) {
                    disabledProjects.add(p.getName());
                    sendNotification(recipients, p, true);
                }
                // If the project is not disabled and we *did* have it in our list of
                // disabled projects...
                else if (!p.isDisabled() && disabledProjects.contains(p.getName())) {
                    disabledProjects.remove(p.getName());
                    sendNotification(recipients, p, false);
                }
                // Otherwise, we don't really care, do we?
            }
        }

    }
    
    private void sendNotification(String recipients, AbstractProject<?, ?> project, boolean isDisabled) {
        try {
            final User currentUser = User.current();
            final String user;
            final String userId;
            if (currentUser != null) {
                user = currentUser.getFullName();
                userId = currentUser.getId();
            } else {
                user = "Anonym";
                userId = "anonymous";
            }
            String baseUrl = Mailer.descriptor().getUrl();
            String projLink = "<a href='" + baseUrl + Util.encode(project.getUrl()) + "'> "
                + project.getName() + "</a>";
                
            if (recipients != null && !recipients.equals("")) {
                Set<InternetAddress> rcp = new LinkedHashSet<InternetAddress>();
                StringTokenizer tokens = new StringTokenizer(recipients);
                while (tokens.hasMoreTokens()) {
                    String address = tokens.nextToken();
                    rcp.add(new InternetAddress(address));
                }
                
                Message msg = new MimeMessage(Mailer.descriptor().createSession());
                msg.setRecipients(Message.RecipientType.TO, rcp.toArray(new InternetAddress[rcp.size()]));
                msg.setFrom(new InternetAddress(Mailer.descriptor().getAdminAddress()));
                if (isDisabled) {
                    msg.setSubject("Build for " + project.getName() + " DISABLED");
                    msg.setText("The build for " + projLink + " has been disabled by " + user + ".");
                }
                else {
                    msg.setSubject("Build for " + project.getName() + " ENABLED");
                    msg.setText("The build for " + projLink + " has been enabled by " + user + ".");
                }
                msg.setHeader("Content-Type", "text/html");
                
                Transport.send(msg);
            }
        } catch (MessagingException e) {
            LOGGER.log(Level.WARNING, "Failed to send out build disabled/enabled notification e-mail");
        }
        
    }
}
