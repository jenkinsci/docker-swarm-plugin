package suryagaddipati.jenkinsdockerslaves;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import java.io.IOException;

public class BuildScheduler {
    public static void scheduleBuild(final Queue.BuildableItem bi, final boolean replace) {
        try {
            final DockerLabelAssignmentAction action = createLabelAssignmentAction();
            if (replace) {
                bi.replaceAction(action);
            } else {
                bi.addAction(action);
            }

            final Node node = new DockerSlave(bi, action.getLabel().toString());
            Computer.threadPoolForRemoting.submit((Runnable) () -> {
                try {
                    Jenkins.getInstance().addNode(node);
                } catch (final IOException e) {
//                        e.printStackTrace();
                }
            });
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final Descriptor.FormException e) {
            e.printStackTrace();
        }
    }

    private static DockerLabelAssignmentAction createLabelAssignmentAction() {
        try {
            Thread.sleep(5, 10);
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }

        final String id = System.nanoTime() + "";
        final Label label = new DockerMachineLabel(id);
        return new DockerLabelAssignmentAction(label);
    }

}
