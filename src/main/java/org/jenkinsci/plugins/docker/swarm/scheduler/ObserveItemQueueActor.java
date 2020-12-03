package org.jenkinsci.plugins.docker.swarm.scheduler;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import hudson.model.Node;
import hudson.model.Queue;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmAgent;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmLabelAssignmentAction;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmAgentSpawner;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmCloud;
import scala.concurrent.duration.Duration;

public class ObserveItemQueueActor extends AbstractActor {
    private static final Logger LOGGER = Logger.getLogger(ObserveItemQueueActor.class.getName());
    private static final long DEFAULT_RESET_MINUTES = 60L;
    public static final int CHECK_INTERVAL = 1;

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchAny(o -> resetStuckBuildsInQueue()).build();
    }

    public static Props props() {
        return Props.create(ObserveItemQueueActor.class, ObserveItemQueueActor::new);
    }

    private void resetStuckBuildsInQueue() throws IOException {
        try {
            long resetMinutes = Optional.ofNullable(DockerSwarmCloud.get().getTimeoutMinutes()).orElse(DEFAULT_RESET_MINUTES);
            final Queue.Item[] items = Jenkins.getInstance().getQueue().getItems();
            for (int i = items.length - 1; i >= 0; i--) { // reverse order
                final Queue.Item item = items[i];
                final DockerSwarmLabelAssignmentAction lblAssignmentAction = item
                        .getAction(DockerSwarmLabelAssignmentAction.class); // This can be null here if computer was
                                                                            // never provisioned. Build will sit in
                                                                            // queue forever
                if (lblAssignmentAction != null) {
                    long inQueueForMinutes = TimeUnit.MILLISECONDS
                            .toMinutes(new Date().getTime() - lblAssignmentAction.getProvisionedTime());
                    if (inQueueForMinutes > resetMinutes) {
                        final String computerName = lblAssignmentAction.getLabel().getName();
                        final Node provisionedNode = Jenkins.getInstance().getNode(computerName);
                        if (provisionedNode != null) {
                            LOGGER.info(String.format("Rescheduling %s and Deleting %s computer ", item, computerName));
                            DockerSwarmAgentSpawner.spawn((Queue.BuildableItem) item);
                            ((DockerSwarmAgent) provisionedNode).terminate();
                        }
                    }
                }
            }
        } finally {
            reschedule();
        }

    }

    private void reschedule() {
        ActorSystem system = getContext().getSystem();
        system.scheduler().scheduleOnce(Duration.apply(CHECK_INTERVAL, TimeUnit.MINUTES), getSelf(), "restart",
                getContext().dispatcher(), ActorRef.noSender());
    }
}
