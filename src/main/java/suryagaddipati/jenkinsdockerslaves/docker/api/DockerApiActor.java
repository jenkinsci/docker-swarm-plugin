package suryagaddipati.jenkinsdockerslaves.docker.api;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.http.scaladsl.marshalling.Marshal;
import akka.stream.ActorMaterializer;
import scala.PartialFunction;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.util.Failure;
import scala.util.Success;
import suryagaddipati.jenkinsdockerslaves.docker.Jackson;

import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;

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

        HttpRequest request = apiRequest.getHttpRequest();
        if(apiRequest.getEntity() ==null){
            executeRequest(request,handleResponse(apiRequest,getSender()));
        }else {
            marshallAndRun(apiRequest.getEntity(), requestEntity -> executeRequest( request.withEntity((RequestEntity) requestEntity), handleResponse(apiRequest,getSender())));
        }
    }

    private BiConsumer<HttpResponse, Throwable> handleResponse(ApiRequest apiRequest, ActorRef sender){
        return (httpResponse, throwable) -> {
            if(throwable != null){
                sender.tell(new ApiException(apiRequest.getClass(),throwable) ,getSelf());
            }
            if(httpResponse.status().isFailure()) {

            }else{
                if(apiRequest.getResponseClass() != null){
                    ActorMaterializer materializer = ActorMaterializer.create(getContext());
                    Unmarshaller<HttpEntity, ?> unmarshaller = Jackson.unmarshaller(apiRequest.getResponseClass());
                    unmarshaller.unmarshal(httpResponse.entity(),materializer).thenApply( csr -> {
                        sender.tell(csr,getSelf());
                        return csr;
                    });
                }
            }
        };

    }


    private  void marshallAndRun(Object object, scala.Function1 andThen){
        ExecutionContextExecutor ec = getContext().dispatcher();
        Marshaller<Object, RequestEntity> marshaller = Jackson.marshaller();
        Future entity = new Marshal(object).to(marshaller.asScala(), ec);
        PartialFunction nextAction = new PartialFunction() {
            @Override
            public boolean isDefinedAt(Object x) {
                return true;
            }
            @Override
            public Object apply(Object result) {
                if(result instanceof Failure){ // This would be caused if there is a bug in code, shouldn't be rescheduled here.
                    Failure failure = (Failure) result;
                    getSender().tell( new ApiResponse.SerializationException(failure.exception()),getSelf()) ;
                    return null;
                }else {
                    return ((Success)result).map(andThen);
                }
            }
        };

        entity.andThen(nextAction,ec);
    }
    private CompletionStage<HttpResponse> executeRequest(HttpRequest req, BiConsumer<HttpResponse, ? super Throwable> onComplete){
        ActorSystem system = getContext().getSystem();
        ActorMaterializer materializer = ActorMaterializer.create(getContext());
        CompletionStage<HttpResponse> rsp = Http.get(system).singleRequest(req, materializer);
        return rsp.whenCompleteAsync(onComplete);

    }
}
