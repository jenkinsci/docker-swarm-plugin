package suryagaddipati.jenkinsdockerslaves;

import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import java.io.IOException;

public class BuildScheduler {
    public static void scheduleBuild(final Queue.BuildableItem bi) {
        try {
            final DockerLabelAssignmentAction action = createLabelAssignmentAction();
            final Node node = new DockerSlave(bi, action.getLabel().toString());
            setToInProgress(bi);
            Jenkins.getInstance().addNode(node); //locks queue
            bi.replaceAction(action);
        } catch (IOException e) {
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

    private static void setToInProgress(final Queue.BuildableItem bi) {
        final DockerSlaveInfo slaveInfoAction = bi.getAction(DockerSlaveInfo.class);
        if (slaveInfoAction != null) {
            slaveInfoAction.setProvisioningInProgress(true);
        } else {
            bi.replaceAction(new DockerSlaveInfo(true));
        }
    }

}
