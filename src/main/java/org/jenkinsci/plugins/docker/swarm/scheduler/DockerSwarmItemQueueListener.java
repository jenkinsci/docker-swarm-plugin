package org.jenkinsci.plugins.docker.swarm.scheduler;

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
import org.jenkinsci.plugins.docker.swarm.DockerSwarmAgent;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmLabelAssignmentAction;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmAgentSpawner;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmCloud;

@Extension
public class DockerSwarmItemQueueListener extends QueueListener {

    private static final Logger LOGGER = Logger.getLogger(DockerSwarmItemQueueListener.class.getName());

    @Override
    public void onEnterBuildable(final Queue.BuildableItem bi) {
        final Queue.Task job = bi.task;
        if (DockerSwarmCloud.get() != null) {
            final List<String> labels = DockerSwarmCloud.get().getLabels();
            if (job.getAssignedLabel() != null && labels.contains(job.getAssignedLabel().getName())) {
                DockerSwarmAgentSpawner.spawn(bi);
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
