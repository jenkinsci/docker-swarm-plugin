package org.jenkinsci.plugins.docker.swarm;

import hudson.model.Label;
import hudson.model.labels.LabelAssignmentAction;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.SubTask;

public class DockerLabelAssignmentAction implements LabelAssignmentAction {

    private final Label label;

    public DockerLabelAssignmentAction(Label label) {
        this.label = label;
    }

    public DockerLabelAssignmentAction(String label){
        this(new LabelAtom(label));
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
}
