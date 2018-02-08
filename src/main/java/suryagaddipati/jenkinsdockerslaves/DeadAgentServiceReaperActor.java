package suryagaddipati.jenkinsdockerslaves;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import suryagaddipati.jenkinsdockerslaves.docker.api.DockerApiActor;
import suryagaddipati.jenkinsdockerslaves.docker.api.DockerApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.nodes.ListNodesRequest;

import java.util.concurrent.CompletionStage;

public class DeadAgentServiceReaperActor extends AbstractActor {
    private final ActorRef apiActor;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchAny(o -> reapDeadAgentServices()).
                        build();
    }
    public static Props props() {
        return Props.create(DeadAgentServiceReaperActor.class, () -> new DeadAgentServiceReaperActor());
    }
    public  DeadAgentServiceReaperActor(){
        this.apiActor = getContext().actorOf(DockerApiActor.props());
    }

    private void reapDeadAgentServices() {
        final CompletionStage<Object> nodesStage = new DockerApiRequest(getContext().getSystem(), new ListNodesRequest()).execute();
    }
}
