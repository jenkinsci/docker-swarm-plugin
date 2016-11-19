package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.Collection;

public class FakeCloudForLabels extends Cloud {
    @DataBoundConstructor
    public FakeCloudForLabels() {
        super("Hack for label validatios");
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(Label label, int excessWorkload) {
        return new ArrayList<NodeProvisioner.PlannedNode>();
    }

    @Override
    public boolean canProvision(Label label) {
        DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
        if (configuration != null) {
            return configuration.canProvision(label);
        }
        return false;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {

        @Override
        public String getDisplayName() {
            return "Jenkins docker slaves(Hack for getting labels in).";
        }
    }
}
