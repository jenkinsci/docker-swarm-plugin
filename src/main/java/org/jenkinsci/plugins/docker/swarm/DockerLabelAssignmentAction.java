package suryagaddipati.jenkinsdockerslaves;

import hudson.model.Label;
import hudson.model.labels.LabelAssignmentAction;
import hudson.model.queue.SubTask;

public class DockerLabelAssignmentAction implements LabelAssignmentAction {

    private final Label label;

    public DockerLabelAssignmentAction(Label label) {
        this.label = label;
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
