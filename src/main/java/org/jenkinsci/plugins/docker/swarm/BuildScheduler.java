package org.jenkinsci.plugins.docker.swarm;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;

public class BuildScheduler {
    private static final Logger LOGGER = Logger.getLogger(BuildScheduler.class.getName());
    private static AtomicInteger counter = new AtomicInteger(1);

    public static void scheduleBuild(final Queue.BuildableItem bi) {
        try (ACLContext _ = ACL.as(ACL.SYSTEM)) {
            final DockerSwarmLabelAssignmentAction action = createLabelAssignmentAction(bi.task.getDisplayName());
            DockerSwarmAgentInfo dockerSwarmAgentInfo = new DockerSwarmAgentInfo(true);
            dockerSwarmAgentInfo.setAgentLabel(action.getLabel().toString());
            bi.replaceAction(dockerSwarmAgentInfo);
            bi.replaceAction(action);
            final Node node = new DockerSwarmAgent(bi, action.getLabel().toString());
            Computer.threadPoolForRemoting.submit(() -> {
                try {
                    Jenkins.getInstance().addNode(node); //locks queue
                } catch (final IOException e) {
                    LOGGER.log(Level.INFO,"couldn't add agent", e);
                }
            });
        } catch (final IOException|Descriptor.FormException e) {
            LOGGER.log(Level.INFO,"couldn't add agent", e);
        }
    }

    private static DockerSwarmLabelAssignmentAction createLabelAssignmentAction(String taskName) {

        try {
            Thread.sleep(5, 10);
        } catch (final InterruptedException e) {
            LOGGER.log(Level.INFO,"couldn't add agent", e);
        }
        taskName = taskName.replaceAll("[^a-zA-Z0-9]", "_");
        if (taskName.length() > 30) {
            taskName = taskName.substring(taskName.length() - 30);
        }
        String truncatedTime = Long.toString(System.nanoTime());
        truncatedTime = truncatedTime.substring(truncatedTime.length() - 10);

        return new DockerSwarmLabelAssignmentAction("agent-" + taskName + "-" +
                truncatedTime + "-" + BuildScheduler.counter.incrementAndGet());
    }
}
