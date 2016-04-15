package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * {@link Cloud} API is designed to launch virtual machines, which is an heavy process, so relies on
 * {@link  NodeProvisioner} to determine when a new slave is required. Here we want the slave to start just as a job
 * enter the build queue. As an alternative we listen the Queue for Jobs to get scheduled, and when label match
 * immediately start a fresh new container executor with a unique label to enforce exclusive usage.
 *
 */
@Extension
public class OneShotProvisionQueueListener extends QueueListener {

    @Override
    public void onEnterBuildable(final Queue.BuildableItem bi) {
        if (bi.task instanceof AbstractProject) {
            AbstractProject job = (AbstractProject) bi.task;
            List<String> labels = DockerSlaveConfiguration.get().getLabels();
            if(! labels.contains( job.getAssignedLabel().getName())){
                return;
            }


            OneshotScheduler.scheduleBuild(bi,false);
        }
    }



    @Override
    public void onLeft(Queue.LeftItem li) {
        if(li.isCancelled()){
            DockerLabelAssignmentAction labelAssignmentAction = li.getAction(DockerLabelAssignmentAction.class);
            if (labelAssignmentAction !=null){
                String computerName = labelAssignmentAction.getLabel().getName();

                final Node node = Jenkins.getInstance().getNode(computerName);
                Computer.threadPoolForRemoting.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Jenkins.getInstance().removeNode(node);
                        } catch (IOException e) {
//                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }


    private static final Logger LOGGER = Logger.getLogger(OneShotProvisionQueueListener.class.getName());
}
