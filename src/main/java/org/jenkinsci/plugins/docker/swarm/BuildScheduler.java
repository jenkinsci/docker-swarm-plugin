package org.jenkinsci.plugins.docker.swarm;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;

public class BuildScheduler {
    private static final Logger LOGGER = Logger.getLogger(BuildScheduler.class.getName());
    private static AtomicInteger counter = new AtomicInteger(1);

    public static void scheduleBuild(final Queue.BuildableItem bi) {
        try (ACLContext context = ACL.as(ACL.SYSTEM)) {
            final DockerSwarmLabelAssignmentAction action = createLabelAssignmentAction(bi.task.getDisplayName());
            DockerSwarmAgentInfo dockerSwarmAgentInfo = new DockerSwarmAgentInfo(true);
            dockerSwarmAgentInfo.setAgentLabel(action.getLabel().toString());
            bi.replaceAction(dockerSwarmAgentInfo);
            bi.replaceAction(action);
            final Node node = new DockerSwarmAgent(bi, action.getLabel().toString());
            Computer.threadPoolForRemoting.submit(() -> {
                try {
                    Jenkins.getInstance().addNode(node); // locks queue
                } catch (final IOException e) {
                    LOGGER.log(Level.INFO, "couldn't add agent", e);
                }
            });
        } catch (final IOException | Descriptor.FormException e) {
            LOGGER.log(Level.INFO, "couldn't add agent", e);
        }
    }

    private static DockerSwarmLabelAssignmentAction createLabelAssignmentAction(String taskName) {
        taskName = taskName.replaceAll("[^a-zA-Z0-9]", "_").replaceAll("_{2,}", "_");
        if (taskName.length() > 15) {
            taskName = taskName.substring(taskName.length() - 15);
        }
        return new DockerSwarmLabelAssignmentAction("agt-" + taskName + "-" +
                Math.abs(BuildScheduler.counter.incrementAndGet() % 9999999));
    }
}
