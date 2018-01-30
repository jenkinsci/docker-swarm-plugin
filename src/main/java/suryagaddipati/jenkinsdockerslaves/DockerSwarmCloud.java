package suryagaddipati.jenkinsdockerslaves;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DockerSwarmCloud extends Cloud {
    private static final String DOCKER_SWARM_CLOUD_NAME = "Docker Swarm";
    String dockerSwarmApiUrl;
    private String jenkinsUrl;
    private String swarmNetwork;
    private String cacheDriverName;
    private List<LabelConfiguration> agentTemplates = new ArrayList<>();

    @DataBoundConstructor
    public DockerSwarmCloud(String dockerSwarmApiUrl, String jenkinsUrl, String swarmNetwork, String cacheDriverName, List<LabelConfiguration> agentTemplates) {
        super(DOCKER_SWARM_CLOUD_NAME);
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

    public LabelConfiguration getLabelConfiguration(final String label) {
        for (final LabelConfiguration labelConfiguration : this.agentTemplates) {
            if (label.equals(labelConfiguration.getLabel())) {
                return labelConfiguration;
            }
        }
        return null;
    }
    public List<String> getLabels() {
        final Iterable<String> labels = Iterables.transform(getAgentTemplates(), labelConfiguration -> labelConfiguration.getLabel());
        return Lists.newArrayList(labels);
    }

    public static DockerSwarmCloud get() {
        return (DockerSwarmCloud ) Jenkins.getInstance().getCloud(DOCKER_SWARM_CLOUD_NAME);
    }

    public void save(){
        getDescriptor().save();
    }
}

