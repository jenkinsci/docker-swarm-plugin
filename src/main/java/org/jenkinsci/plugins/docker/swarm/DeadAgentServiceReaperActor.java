package org.jenkinsci.plugins.docker.swarm;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiException;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.SerializationException;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.DeleteServiceRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ListServicesRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ScheduledService;
import org.jenkinsci.plugins.docker.swarm.docker.api.task.ListTasksRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.task.Task;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import jenkins.model.Jenkins;
import scala.concurrent.duration.Duration;

public class DeadAgentServiceReaperActor extends AbstractActor {
    private static final Logger LOGGER = Logger.getLogger(DeadAgentServiceReaperActor.class.getName());

    @Override
    public Receive createReceive() {
        return receiveBuilder().matchAny(o -> reapDeadAgentServices()).build();
    }

    public static Props props() {
        return Props.create(DeadAgentServiceReaperActor.class, () -> new DeadAgentServiceReaperActor());
    }

    private void reapDeadAgentServices() throws IOException {
        try {
            final DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
            final ActorSystem as = swarmPlugin.getActorSystem();
            if (DockerSwarmCloud.get() != null) {
                String dockerSwarmApiUrl = DockerSwarmCloud.get().getDockerSwarmApiUrl();
                final Object result = new ListServicesRequest(dockerSwarmApiUrl, "label", "ROLE=jenkins-agent").execute();
                for (ScheduledService service : (List<ScheduledService>) getResult(result, List.class)) {
                    Object tasks = new ListTasksRequest(dockerSwarmApiUrl, "service", service.Spec.Name).execute();
                    if (tasks != null) {
                        for (Task task : (List<Task>) getResult(tasks, List.class)) {
                            if (task.isComplete()) {
                                LOGGER.info("Reaping service: " + service.Spec.Name);
                                new DeleteServiceRequest(dockerSwarmApiUrl, service.Spec.Name).execute();
                                break;
                            }
                        }
                    }

                }
            }
        } finally {
            reschedule();
        }

    }

    private <T> T getResult(Object result, Class<T> clazz) {
        if (result instanceof SerializationException) {
            throw new RuntimeException(((SerializationException) result).getCause());
        }
        if (result instanceof ApiException) {
            throw new RuntimeException(((ApiException) result).getCause());
        }
        return clazz.cast(result);
    }

    private void reschedule() {
        ActorSystem system = getContext().getSystem();
        system.scheduler().scheduleOnce(Duration.apply(5, TimeUnit.MINUTES), getSelf(), "restart",
                getContext().dispatcher(), ActorRef.noSender());
    }
}
