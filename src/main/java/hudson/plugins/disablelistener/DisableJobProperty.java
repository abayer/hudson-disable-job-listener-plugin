package hudson.plugins.disablelistener;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Job;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

public class DisableJobProperty extends JobProperty<AbstractProject<?, ?>> {
    private String recipients;

    @DataBoundConstructor
    public DisableJobProperty(String recipients) {
        this.recipients = recipients;
    }

    public String getRecipients() {
        return recipients;
    }

    public boolean getNotify() {
        if (recipients != null && !recipients.equals("")) {
            return true;
        }
        else {
            return false;
        }
    }


    @Extension
    public static final class DescriptorImpl extends JobPropertyDescriptor {
        public DescriptorImpl() {
            super(DisableJobProperty.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Notify When Job Is Disabled";
        }
        
        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

    }
}