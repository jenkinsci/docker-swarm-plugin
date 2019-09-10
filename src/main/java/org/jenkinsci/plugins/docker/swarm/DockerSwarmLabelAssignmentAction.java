package org.jenkinsci.plugins.docker.swarm;

import java.util.Date;

import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAssignmentAction;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.SubTask;

public class DockerSwarmLabelAssignmentAction implements LabelAssignmentAction {

    private final Label label;
    private final long provisionedTime;

    public DockerSwarmLabelAssignmentAction(Label label) {
        this.label = label;
        this.provisionedTime = new Date().getTime();
    }

    public DockerSwarmLabelAssignmentAction(String label) {
        this(new DockerSwarmAgentLabel(label));
    }

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getUrlName() {
        return null;
    }

    @Override
    public Label getAssignedLabel(SubTask task) {
        return label;
    }

    public Label getLabel() {
        return label;
    }

    private static class DockerSwarmAgentLabel extends LabelAtom {
        public DockerSwarmAgentLabel(String name) {
            super(name);
        }

        @Override
        public boolean contains(Node node) {
            return this.name.equals(node.getNodeName());
        }
    }

    public long getProvisionedTime() {
        return provisionedTime;
    }
}
