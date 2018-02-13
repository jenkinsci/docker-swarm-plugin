package org.jenkinsci.plugins.docker.swarm.dashboard;

import hudson.model.Computer;
import hudson.model.Queue;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.swarm.DockerLabelAssignmentAction;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmAgentInfo;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmCloud;
import org.jenkinsci.plugins.docker.swarm.LabelConfiguration;

public class SwarmQueueItem {

    private final String name;
    private final String label;
    private final LabelConfiguration labelConfig;
    private final String inQueueSince;
    private final DockerSwarmAgentInfo agentInfo;
    private Computer provisionedComputer;

    public SwarmQueueItem(final Queue.BuildableItem item) {
        this.name = item.task.getFullDisplayName();
        this.label = item.task.getAssignedLabel().getName();
        this.labelConfig = DockerSwarmCloud.get().getLabelConfiguration(this.label);
        this.inQueueSince = item.getInQueueForString();
        this.agentInfo = item.getAction(DockerSwarmAgentInfo.class); //this should never be null

        final DockerLabelAssignmentAction lblAssignmentAction = item.getAction(DockerLabelAssignmentAction.class);
        if (lblAssignmentAction != null) {
            final String computerName = lblAssignmentAction.getLabel().getName();
            this.provisionedComputer = Jenkins.getInstance().getComputer(computerName);
        }
    }

    public Computer getProvisionedComputer() {
        return this.provisionedComputer;
    }

    public DockerSwarmAgentInfo getAgentInfo() {
        return this.agentInfo;
    }

    public String getName() {
        return this.name;
    }

    public String getLabel() {
        return this.label;
    }

    public LabelConfiguration getLabelConfig() {
        return this.labelConfig;
    }

    public String getInQueueSince() {
        return this.inQueueSince;
    }
}
