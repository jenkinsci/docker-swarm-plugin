package org.jenkinsci.plugins.docker.swarm;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.QueueListener;
import jenkins.model.Jenkins;

@Extension
public class OneShotProvisionQueueListener extends QueueListener {

    private static final Logger LOGGER = Logger.getLogger(OneShotProvisionQueueListener.class.getName());

    @Override
    public void onEnterBuildable(final Queue.BuildableItem bi) {
        final Queue.Task job = bi.task;
        if (DockerSwarmCloud.get() != null) {
            final List<String> labels = DockerSwarmCloud.get().getLabels();
            if (job.getAssignedLabel() != null && labels.contains(job.getAssignedLabel().getName())) {
                BuildScheduler.scheduleBuild(bi);
            }
        }
    }

    @Override
    public void onLeft(final Queue.LeftItem li) {
        if (li.isCancelled()) {
            final DockerSwarmLabelAssignmentAction labelAssignmentAction = li
                    .getAction(DockerSwarmLabelAssignmentAction.class);
            if (labelAssignmentAction != null) {
                final String computerName = labelAssignmentAction.getLabel().getName();

                final Node node = Jenkins.getInstance().getNode(computerName);
                Computer.threadPoolForRemoting.submit(() -> {
                    try {
                        ((DockerSwarmAgent) node).terminate();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to terminate agent.", e);
                    }
                });
            }
        }
    }

}
