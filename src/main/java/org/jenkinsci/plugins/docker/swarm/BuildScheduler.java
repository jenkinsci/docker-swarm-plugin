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

public class BuildScheduler {
    private static final Logger LOGGER = Logger.getLogger(BuildScheduler.class.getName());
    public static void scheduleBuild(final Queue.BuildableItem bi) {
        try (ACLContext _ = ACL.as(ACL.SYSTEM)) {
            final DockerSwarmLabelAssignmentAction action = createLabelAssignmentAction();
            final Node node = new DockerSwarmAgent(bi, action.getLabel().toString());
            bi.replaceAction(new DockerSwarmAgentInfo(true));
            bi.replaceAction(action);
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

    private static DockerSwarmLabelAssignmentAction createLabelAssignmentAction() {
        try {
            Thread.sleep(5, 10);
        } catch (final InterruptedException e) {
            LOGGER.log(Level.INFO,"couldn't add agent", e);
        }

        return new DockerSwarmLabelAssignmentAction("agent-" + System.nanoTime());
    }
}
