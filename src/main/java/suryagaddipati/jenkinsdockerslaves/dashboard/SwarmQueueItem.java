package suryagaddipati.jenkinsdockerslaves.dashboard;

import hudson.model.Computer;
import hudson.model.Queue;
import jenkins.model.Jenkins;
import suryagaddipati.jenkinsdockerslaves.DockerLabelAssignmentAction;
import suryagaddipati.jenkinsdockerslaves.DockerSlaveInfo;
import suryagaddipati.jenkinsdockerslaves.DockerSwarmCloud;
import suryagaddipati.jenkinsdockerslaves.LabelConfiguration;

public class SwarmQueueItem {

    private final String name;
    private final String label;
    private final LabelConfiguration labelConfig;
    private final String inQueueSince;
    private final DockerSlaveInfo slaveInfo;
    private Computer provisionedComputer;

    public SwarmQueueItem(final Queue.BuildableItem item) {
        this.name = item.task.getFullDisplayName();
        this.label = item.task.getAssignedLabel().getName();
        this.labelConfig = DockerSwarmCloud.get().getLabelConfiguration(this.label);
        this.inQueueSince = item.getInQueueForString();
        this.slaveInfo = item.getAction(DockerSlaveInfo.class); //this should never be null

        final DockerLabelAssignmentAction lblAssignmentAction = item.getAction(DockerLabelAssignmentAction.class);
        if (lblAssignmentAction != null) {
            final String computerName = lblAssignmentAction.getLabel().getName();
            this.provisionedComputer = Jenkins.getInstance().getComputer(computerName);
        }
    }

    public Computer getProvisionedComputer() {
        return this.provisionedComputer;
    }

    public DockerSlaveInfo getSlaveInfo() {
        return this.slaveInfo;
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
