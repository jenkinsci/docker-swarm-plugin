package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

@Extension
public class OneShotProvisionQueueListener extends QueueListener {

    @Override
    public void onEnterBuildable(final Queue.BuildableItem bi) {
        if (bi.task instanceof AbstractProject) {
            AbstractProject job = (AbstractProject) bi.task;
            List<String> labels = DockerSlaveConfiguration.get().getLabels();
            if(job.getAssignedLabel() != null && labels.contains( job.getAssignedLabel().getName())){
                BuildScheduler.scheduleBuild(bi,false);
            }


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

}
