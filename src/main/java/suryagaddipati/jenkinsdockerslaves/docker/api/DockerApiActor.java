package suryagaddipati.jenkinsdockerslaves.docker.api;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import suryagaddipati.jenkinsdockerslaves.docker.api.request.ApiRequest;
//TODO delete this class
public class DockerApiActor extends AbstractActor {

    public static Props props() {
        return Props.create(DockerApiActor.class, () -> new DockerApiActor());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ApiRequest.class, apiRequest -> executeApiRequest(apiRequest))
                .build();
    }

    private void executeApiRequest(ApiRequest apiRequest) {
        ActorSystem as = getContext().getSystem();
        ActorRef sender = getSender();
        new DockerApiRequest(as,apiRequest).execute().thenAcceptAsync( x -> {
            sender.tell(x,sender );

        });
    }
}
