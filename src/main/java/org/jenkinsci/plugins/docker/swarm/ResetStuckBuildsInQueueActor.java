package org.jenkinsci.plugins.docker.swarm;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.slaves.Cloud;
import jenkins.model.Jenkins;
import scala.concurrent.duration.Duration;

public class ResetStuckBuildsInQueueActor extends AbstractActor {
    private static final Logger LOGGER = Logger.getLogger(ResetStuckBuildsInQueueActor.class.getName());
    private static final long DEFAULT_RESET_MINUTES = 60L;
    public static final int CHECK_INTERVAL = 1;

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchAny(o -> resetStuckBuildsInQueue()).build();
    }

    public static Props props() {
        return Props.create(ResetStuckBuildsInQueueActor.class, ResetStuckBuildsInQueueActor::new);
    }

    private void resetStuckBuildsInQueue() throws IOException {
        try {
            //FIXME getting timeout from first cloud
            DockerSwarmCloud swarmCloud = null;
            for (final Cloud cloud: Jenkins.getInstance().clouds) {
                if(cloud instanceof DockerSwarmCloud) {
                    swarmCloud = (DockerSwarmCloud)cloud;
                    break;
                }
            }
            long resetMinutes = Optional.ofNullable(swarmCloud.getTimeoutMinutes()).orElse(DEFAULT_RESET_MINUTES);
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
                            //FIXME copy paste from onEnterBuildable
                            //BuildScheduler.scheduleBuild((Queue.BuildableItem) item, DockerSwarmCloud.get());
                            final Queue.BuildableItem bi = (Queue.BuildableItem) item;
                            final Jenkins jenkins = Jenkins.getInstance();
                            final Label label = bi.getAssignedLabel();
                            for (Cloud cloud : jenkins.clouds) {
                                LOGGER.log(Level.WARNING, "Checking cloud " + cloud.name + " url " + cloud.toString());
                                if (cloud instanceof DockerSwarmCloud && cloud.canProvision(label)) {
                                    BuildScheduler.scheduleBuild(bi, (DockerSwarmCloud)cloud);
                                    break;
                                }
                            }
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
