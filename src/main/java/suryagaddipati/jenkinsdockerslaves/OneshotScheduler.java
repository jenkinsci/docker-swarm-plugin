package suryagaddipati.jenkinsdockerslaves;

import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import java.io.IOException;

public class OneshotScheduler {
    public  static void scheduleBuild(Queue.BuildableItem bi, boolean replace) {
        AbstractProject job = (AbstractProject) bi.task;
        try {
            DockerLabelAssignmentAction action = createLabelAssignmentAction();
            if(replace){
               bi.replaceAction(action);
            }else {
                bi.addAction(action);
            }
            // Immediately create a slave for this item
            // Real provisioning will happen later

            final Node node = new DockerSlave(bi, action.getLabel().toString());
            Computer.threadPoolForRemoting.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Jenkins.getInstance().addNode(node);
                    } catch (IOException e) {
//                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Descriptor.FormException e) {
            e.printStackTrace();
        }
    }
    private static DockerLabelAssignmentAction createLabelAssignmentAction() {
        try {
            Thread.sleep(5,10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        final String id = Long.toHexString(System.nanoTime());
        final Label label = new DockerMachineLabel(id);
        return new DockerLabelAssignmentAction(label);
    }

}
