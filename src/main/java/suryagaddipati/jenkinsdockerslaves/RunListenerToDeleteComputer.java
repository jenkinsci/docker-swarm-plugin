package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.io.PrintStream;

@Extension
public class RunListenerToDeleteComputer extends RunListener<Run<?, ?>> {

    @Override
    public void onCompleted(final Run<?, ?> run, @Nonnull final TaskListener listener) {
        if (run.getAction(DockerLabelAssignmentAction.class) != null) {
            final DockerLabelAssignmentAction labelAssignmentAction = run.getAction(DockerLabelAssignmentAction.class);
            final String computerName = labelAssignmentAction.getLabel().getName();
            final PrintStream logger = listener.getLogger();
            final DockerComputer computer = (DockerComputer) Jenkins.getInstance().getComputer(computerName);
            computer.destroyContainer(logger);
        }
    }
}
