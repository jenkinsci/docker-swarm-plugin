package org.jenkinsci.plugins.docker.swarm;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jenkinsci.plugins.docker.swarm.docker.api.service.DeleteServiceRequest;

import akka.actor.ActorRef;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.CauseOfBlockage;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.EphemeralNode;
import jenkins.model.Jenkins;

public class DockerSwarmAgent extends AbstractCloudSlave implements EphemeralNode {

    private static final Logger LOGGER = Logger.getLogger(DockerSwarmAgent.class.getName());

    private final DockerSwarmAgentTemplate template;
    private final String cloudName;

    public DockerSwarmAgent(final Queue.BuildableItem bi, final String labelString, final DockerSwarmCloud cloud)
            throws Descriptor.FormException, IOException {
        super(labelString, "Docker swarm agent for building " + bi.task.getFullDisplayName(),
                cloud.getLabelConfiguration(bi.task.getAssignedLabel().getName()).getWorkingDir(), 1,
                Mode.EXCLUSIVE, labelString, new DockerSwarmComputerLauncher(bi, cloud),
                new DockerSwarmAgentRetentionStrategy(1), Collections.emptyList());
        final DockerSwarmAgentTemplate labelConfiguration = cloud.getLabelConfiguration(bi.task.getAssignedLabel().getName());
        this.template = labelConfiguration;
        this.cloudName = cloud.name;
        LOGGER.log(Level.FINE, "Created docker swarm agent: {0}", labelString);
    }

    public String getCloudName() {
        return cloudName;
    }

    public DockerSwarmComputer createComputer() {
        return new DockerSwarmComputer(this);
    }

    public DockerSwarmAgentTemplate getTemplate() {
        return template;
    }

    @Override
    protected void _terminate(final TaskListener listener) throws IOException, InterruptedException {
    }

    @Override
    public Node asNode() {
        return this;
    }

    @Override
    public CauseOfBlockage canTake(final Queue.BuildableItem item) {
        final Label l = item.getAssignedLabel();
        if (l != null && this.name.equals(l.getName())) {
            return null;
        }
        return super.canTake(item);
    }

    @Override
    public Set<LabelAtom> getAssignedLabels() {
        final TreeSet<LabelAtom> labels = new TreeSet<>();
        labels.add(new LabelAtom(getLabelString()));
        return labels;
    }

    public void terminate() throws IOException {
        try {
            DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
            ActorRef agentLauncherRef = swarmPlugin.getActorSystem().actorFor("/user/" + getComputer().getName());
            agentLauncherRef.tell(new DeleteServiceRequest(getComputer().getName()), ActorRef.noSender());
        } finally {
            try {
                Jenkins.getInstance().removeNode(this);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to remove computer", e);
            }
        }
    }
}
