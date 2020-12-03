package org.jenkinsci.plugins.docker.swarm;

import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.security.ACL;
import hudson.security.ACLContext;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.swarm.utils.Util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerSwarmAgentSpawner {
    private static final Logger LOGGER = Logger.getLogger(DockerSwarmAgentSpawner.class.getName());

    public static void spawn(final Queue.BuildableItem bi) {
        try (ACLContext _ = ACL.as(ACL.SYSTEM)) {
            final DockerSwarmLabelAssignmentAction action = createLabelAssignmentAction(bi);
            DockerSwarmAgentInfo dockerSwarmAgentInfo = new DockerSwarmAgentInfo(true);
            dockerSwarmAgentInfo.setAgentLabel(action.getLabel().getName());
            bi.replaceAction(dockerSwarmAgentInfo);
            bi.replaceAction(action);

            final Node node = new DockerSwarmAgent(bi, action.getLabel().getName());
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

    private static DockerSwarmLabelAssignmentAction createLabelAssignmentAction(final Queue.BuildableItem bi) {
        String taskName = Util.codenamizeTask(bi.task);
        return new DockerSwarmLabelAssignmentAction(taskName);
    }
}
