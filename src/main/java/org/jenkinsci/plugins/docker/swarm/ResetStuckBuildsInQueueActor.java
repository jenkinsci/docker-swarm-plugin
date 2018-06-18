package org.jenkinsci.plugins.docker.swarm;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import hudson.model.Node;
import hudson.model.Queue;
import jenkins.model.Jenkins;
import scala.concurrent.duration.Duration;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class ResetStuckBuildsInQueueActor extends AbstractActor {
    private static final Logger LOGGER = Logger.getLogger(ResetStuckBuildsInQueueActor.class.getName());
    public static final int RESET_MINUTES = 5;
    public static final int CHECK_INTERVAL = 1;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(o -> resetStuckBuildsInQueue()).
                        build();
    }
    public static Props props() {
        return Props.create(ResetStuckBuildsInQueueActor.class, () -> new ResetStuckBuildsInQueueActor());
    }

    private void resetStuckBuildsInQueue() throws IOException {
        try{
            final Queue.Item[] items = Jenkins.getInstance().getQueue().getItems();
            for (int i = items.length - 1; i >= 0; i--) { //reverse order
                final Queue.Item item = items[i];
                final DockerSwarmLabelAssignmentAction lblAssignmentAction = item.getAction(DockerSwarmLabelAssignmentAction.class); // This can be null here if computer was never provisioned. Build will sit in queue forever
                if(lblAssignmentAction != null){
                    long inQueueForMinutes = TimeUnit.MILLISECONDS.toMinutes(new Date().getTime() - lblAssignmentAction.getProvisionedTime());
                    if(inQueueForMinutes > RESET_MINUTES){
                        final String computerName = lblAssignmentAction.getLabel().getName();
                        final Node provisionedNode = Jenkins.getInstance().getNode(computerName);
                        if(provisionedNode!= null){
                            LOGGER.info(String.format("Rescheduling %s and Deleting %s computer ",item, computerName));
                            BuildScheduler.scheduleBuild((Queue.BuildableItem) item);
                            ((DockerSwarmAgent)provisionedNode).terminate();
                        }
                    }
                }
            }
        }finally {
            resechedule();
        }

    }

    private void resechedule() {
        ActorSystem system = getContext().getSystem();
        system.scheduler().scheduleOnce(Duration.apply(CHECK_INTERVAL, TimeUnit.MINUTES),getSelf(),"restart", getContext().dispatcher(), ActorRef.noSender());
    }
}
