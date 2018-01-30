package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DockerSwarmCloud extends Cloud {
    String dockerSwarmApiUrl;
    private String jenkinsUrl;
    private String swarmNetwork;
    private String cacheDriverName;
    private List<LabelConfiguration> agentTemplates = new ArrayList<>();

    @DataBoundConstructor
    public DockerSwarmCloud(String dockerSwarmApiUrl, String jenkinsUrl, String swarmNetwork, String cacheDriverName, List<LabelConfiguration> agentTemplates) {
        super("Docker Swarm");
        this.dockerSwarmApiUrl = dockerSwarmApiUrl;
        this.jenkinsUrl = jenkinsUrl;
        this.swarmNetwork = swarmNetwork;
        this.cacheDriverName = cacheDriverName;
        this.agentTemplates = agentTemplates;
    }


    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(final Label label, final int excessWorkload) {
        return new ArrayList<>();
    }


    @Override
    public boolean canProvision(final Label label) {
        return false;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {

        @Override
        public String getDisplayName() {
            return "Docker Swarm";
        }
    }

    public String getDockerSwarmApiUrl() {
        return dockerSwarmApiUrl;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public String getSwarmNetwork() {
        return swarmNetwork;
    }

    public String getCacheDriverName() {
        return cacheDriverName;
    }

    public List<LabelConfiguration> getAgentTemplates() {
        return agentTemplates;
    }
}
