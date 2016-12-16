package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;

@Extension
public class OneShotProvisionQueueListener extends QueueListener {

    @Override
    public void onEnterBuildable(final Queue.BuildableItem bi) {
        final Queue.Task job = bi.task;
        final List<String> labels = DockerSlaveConfiguration.get().getLabels();
        if (job.getAssignedLabel() != null && labels.contains(job.getAssignedLabel().getName())) {
            BuildScheduler.scheduleBuild(bi, false);
        }
    }


    @Override
    public void onLeft(final Queue.LeftItem li) {
        if (li.isCancelled()) {
            final DockerLabelAssignmentAction labelAssignmentAction = li.getAction(DockerLabelAssignmentAction.class);
            if (labelAssignmentAction != null) {
                final String computerName = labelAssignmentAction.getLabel().getName();

                final Node node = Jenkins.getInstance().getNode(computerName);
                Computer.threadPoolForRemoting.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Jenkins.getInstance().removeNode(node);
                        } catch (final IOException e) {
//                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

}
