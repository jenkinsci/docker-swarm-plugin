package org.jenkinsci.plugins.docker.swarm;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.docker.swarm.docker.api.service.DeleteServiceRequest;

import akka.actor.ActorRef;
import hudson.model.Descriptor;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudSlave;
import jenkins.model.Jenkins;

public class DockerSwarmAgent extends AbstractCloudSlave {

    private static final Logger LOGGER = Logger.getLogger(DockerSwarmAgent.class.getName());

    public DockerSwarmAgent(final Queue.BuildableItem bi, final String labelString)
            throws Descriptor.FormException, IOException {
        super(labelString, "Docker swarm agent for building " + bi.task.getFullDisplayName(),
                DockerSwarmCloud.get().getLabelConfiguration(bi.task.getAssignedLabel().getName()).getWorkingDir(), 1,
                Mode.EXCLUSIVE, labelString, new DockerSwarmComputerLauncher(bi),
                new DockerSwarmAgentRetentionStrategy(1), Collections.emptyList());
        LOGGER.log(Level.FINE, "Created docker swarm agent: {0}", labelString);
    }

    public DockerSwarmComputer createComputer() {
        return new DockerSwarmComputer(this);
    }

    @Override
    protected void _terminate(final TaskListener listener) throws IOException, InterruptedException {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        ActorRef agentLauncherRef = swarmPlugin.getActorSystem().actorFor("/user/" + getComputer().getName());
        agentLauncherRef.tell(new DeleteServiceRequest(getComputer().getName()), ActorRef.noSender());
    }

}
