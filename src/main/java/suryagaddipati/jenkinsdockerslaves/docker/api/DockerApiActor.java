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
import scala.Function1;
import scala.PartialFunction;
import scala.concurrent.ExecutionContext;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.util.Either;
import scala.util.Failure;
import scala.util.Left;
import scala.util.Right;
import scala.util.Success;
import suryagaddipati.jenkinsdockerslaves.docker.api.request.ApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiError;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiException;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiSuccess;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.SerializationException;
import suryagaddipati.jenkinsdockerslaves.docker.marshalling.Jackson;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

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
//        marshallAndRun(apiRequest.getEntity(), requestEntity -> executeRequest( request.withEntity((RequestEntity) requestEntity), handleResponse(apiRequest,getSender())));
    }

    private Object marshallResponse(CompletionStage<Either<SerializationException, HttpResponse>> httpResponseStage) {
        return httpResponseStage.thenApply( response -> {
            if(response.isRight()){

            }
            return CompletableFuture.completedFuture( new Left( response.left().get()));
        });
    }

    private CompletionStage<Either<SerializationException, HttpResponse>>  executeRequest(Either<SerializationException, RequestEntity> marshallResult, HttpRequest request, ActorSystem system) {
        if(marshallResult.isRight()){
            ActorMaterializer materializer = ActorMaterializer.create(system);
            RequestEntity requestEntity = marshallResult.right().get();
            CompletionStage<HttpResponse> rsp = Http.get(system).singleRequest(request.withEntity(requestEntity), materializer);
            return rsp.thenApply(rsp1 -> new Right(rsp1));
        }
        return CompletableFuture.completedFuture( new Left( marshallResult.left().get()));
    }

    private CompletionStage<Object> executeRequest(HttpRequest req, BiFunction< ? super HttpResponse, Throwable,Object> onComplete){
        ActorSystem system = getContext().getSystem();
        ActorMaterializer materializer = ActorMaterializer.create(system);
        CompletionStage<HttpResponse> rsp = Http.get(system).singleRequest(req, materializer);
        return rsp.handleAsync(onComplete);

    }

    private BiFunction< ? super HttpResponse, Throwable,Object> handleResponse(ApiRequest apiRequest, ActorRef sender){
        return (httpResponse, throwable) -> {
            if(throwable != null){
                sender.tell(new ApiException(apiRequest.getClass(),throwable) ,getSelf());
            }else {

                ActorMaterializer materializer = ActorMaterializer.create(getContext());
                if(httpResponse.status().isFailure()) {
                    CompletionStage<Object> x = handleFailure(apiRequest, sender, httpResponse, materializer);
                }else{
                    handleSuccess(apiRequest, sender, httpResponse, materializer);
                }
            }
            return "";
        };

    }

    private CompletionStage<Object> handleFailure(ApiRequest apiRequest, ActorRef sender, HttpResponse httpResponse, ActorMaterializer materializer) {

        Unmarshaller<HttpEntity, ErrorMessage> unmarshaller = Jackson.unmarshaller(ErrorMessage.class);
       return  unmarshaller.unmarshal(httpResponse.entity(),materializer).<Object>thenApplyAsync(csr -> {
           ApiError value = new ApiError(apiRequest.getClass(), httpResponse.status(), csr.message);
            sender.tell(value,getSelf());
            return value;
        }).exceptionally(throwable -> {
           SerializationException value = new SerializationException(throwable);
            sender.tell(value,getSelf());
            return value;
        } );
    }

    private CompletionStage<Object> handleSuccess(ApiRequest apiRequest, ActorRef sender, HttpResponse httpResponse, ActorMaterializer materializer) {
        if(apiRequest.getResponseClass() != null){
            Unmarshaller<HttpEntity, ?> unmarshaller = Jackson.unmarshaller(apiRequest.getResponseClass(), apiRequest.getResponseType());
            return unmarshaller.unmarshal(httpResponse.entity(),materializer).thenApply( csr -> {
                sender.tell(csr,getSelf());
                return csr;
            });
        }else{
            ApiSuccess value = new ApiSuccess(apiRequest.getClass(), httpResponse.entity());
            sender.tell(value,getSelf());
            return CompletableFuture.completedFuture(value);
        }
    }

    private Future<Either<SerializationException,RequestEntity>> marshall(Object object, ExecutionContext ec){
        Marshaller<Object, RequestEntity> marshaller = Jackson.marshaller();
        Future entity = new Marshal(object).to(marshaller.asScala(), ec);
        PartialFunction nextAction = new PartialFunction() {
            @Override
            public boolean isDefinedAt(Object x) {
                return true;
            }
            @Override
            public Object apply(Object result) {
                if(result instanceof Failure){
                    Failure failure = (Failure) result;
                    return new Left(new SerializationException(failure.exception()));
                }else {
                    return new Right( ((Success)result).value());
                }
            }
        };
        return entity.map(nextAction, ec);
    }
    private Future<Either<SerializationException,RequestEntity>> marshallAndRun(Object object, Function1 andThen){
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
                if(result instanceof Failure){
                    Failure failure = (Failure) result;
                    SerializationException value = new SerializationException(failure.exception());
                    getSender().tell(value,getSelf()) ;
                    return new Left( value);
                }else {
                     ((Success)result).map(andThen);
                     return new Right( ((Success)result).value());
                }
            }
        };
        return entity.map(nextAction,ec);
    }

//    private Future marshall(Object object, Function1 andThen){
//
//    }


    private static class ErrorMessage{
        public String message;
    }
}
