package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class OneShotProvisionQueueListener extends QueueListener {

    private static final Logger LOGGER = Logger.getLogger(OneShotProvisionQueueListener.class.getName());
    @Override
    public void onEnterBuildable(final Queue.BuildableItem bi) {
        final Queue.Task job = bi.task;
        final List<String> labels = DockerSlaveConfiguration.get().getLabels();
        if (job.getAssignedLabel() != null && labels.contains(job.getAssignedLabel().getName())) {
            BuildScheduler.scheduleBuild(bi);
        }
    }


    @Override
    public void onLeft(final Queue.LeftItem li) {
        if (li.isCancelled()) {
            final DockerLabelAssignmentAction labelAssignmentAction = li.getAction(DockerLabelAssignmentAction.class);
            if (labelAssignmentAction != null) {
                final String computerName = labelAssignmentAction.getLabel().getName();

                final Node node = Jenkins.getInstance().getNode(computerName);
                Computer.threadPoolForRemoting.submit(() -> {
                    try {
                        ((DockerSlave)node).terminate();
                    } catch (final IOException | InterruptedException e) {
                       LOGGER.log(Level.SEVERE,"",e);
                    }
                });
            }
        }
    }

}
