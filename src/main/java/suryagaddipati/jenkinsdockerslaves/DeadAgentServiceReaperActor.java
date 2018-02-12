package suryagaddipati.jenkinsdockerslaves;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import jenkins.model.Jenkins;
import suryagaddipati.jenkinsdockerslaves.docker.api.DockerApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiException;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.SerializationException;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.DeleteServiceRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.ListServicesRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.ScheduledService;
import suryagaddipati.jenkinsdockerslaves.docker.api.task.ListTasksRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.task.Task;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DeadAgentServiceReaperActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(o -> reapDeadAgentServices()).
                        build();
    }
    public static Props props() {
        return Props.create(DeadAgentServiceReaperActor.class, () -> new DeadAgentServiceReaperActor());
    }

    private void reapDeadAgentServices() {
        final DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        final ActorSystem as = swarmPlugin.getActorSystem();
        String dockerSwarmApiUrl = DockerSwarmCloud.get().getDockerSwarmApiUrl();
        final CompletionStage<Object> nodesStage = new DockerApiRequest(as, new ListServicesRequest(dockerSwarmApiUrl,"label","ROLE=jenkins-agent")).execute();
        CompletionStage<Object> tasksStage = nodesStage.thenApplyAsync(services -> {
            if(services instanceof ApiException){
                return services;
            }
            for(ScheduledService service : (List<ScheduledService>)services ){
                CompletionStage<Object> tasksRequest = new DockerApiRequest(as, new ListTasksRequest(dockerSwarmApiUrl, "service", service.Spec.Name)).execute();
                List<Task> tasks = getFuture(tasksRequest, List.class);
                if(tasks != null && tasks.size()==1) {
                    Task task = tasks.get(0);
                    if(task.isComplete()){
                        CompletionStage<Object> deleteServiceRequest = new DockerApiRequest(as, new DeleteServiceRequest(dockerSwarmApiUrl,service.Spec.Name)).execute();
                        getFuture(deleteServiceRequest,Object.class);
                    }
                }

                System.out.println(service.Spec.Name);
            }
            return services;
        });
        Object o = getFuture(tasksStage,Object.class);
        if(o instanceof  ApiException){
            ((ApiException)o).getCause().printStackTrace();
        }
    }
    private <T> T  getFuture(final CompletionStage<Object> future,Class<T> clazz) {
        try {
            final Object result = future.toCompletableFuture().get(5, TimeUnit.SECONDS);
            return getResult(result,clazz);
        } catch (InterruptedException|ExecutionException |TimeoutException e) {
            throw  new RuntimeException(e);
        }
    }
    private <T> T  getResult(Object result, Class<T> clazz){
        if(result instanceof SerializationException){
            throw new RuntimeException (((SerializationException)result).getCause());
        }
        if(result instanceof ApiException){
            throw new RuntimeException (((ApiException)result).getCause());
        }
        return clazz.cast(result);
    }
}
