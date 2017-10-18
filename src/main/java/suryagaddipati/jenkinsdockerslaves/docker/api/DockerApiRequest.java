package suryagaddipati.jenkinsdockerslaves.docker.api;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.ActorMaterializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;
import suryagaddipati.jenkinsdockerslaves.docker.api.request.ApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiError;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiSuccess;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.SerializationException;
import suryagaddipati.jenkinsdockerslaves.docker.marshalling.Jackson;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class DockerApiRequest {
    private final ActorSystem as;
    private final ApiRequest apiRequest;
    private final ActorMaterializer materializer;

    public DockerApiRequest( ActorSystem as, ApiRequest request) {
        this.as = as;
        this.materializer = ActorMaterializer.create(as);
        this.apiRequest = request;
    }

    public CompletionStage<Object> execute(){
        return marshall(apiRequest.getEntity())
                .thenComposeAsync( marshallResult -> executeRequest( marshallResult, apiRequest.getHttpRequest()))
                .thenComposeAsync( httpResponse -> marshallResponse(httpResponse))
                .exceptionally(ex ->new SerializationException(ex));
    }


    private CompletionStage<Object> marshallResponse(Either<SerializationException, HttpResponse> response) {
        if(response.isRight()){
            HttpResponse httpResponse = response.right().get();
            return httpResponse.status().isFailure()?  handleFailure(httpResponse) : handleSuccess( httpResponse);
        }
        return   CompletableFuture.completedFuture( response.left().get());
    }

    private CompletionStage<Object> handleSuccess(HttpResponse httpResponse) {
        if(apiRequest.getResponseClass() != null){
            Unmarshaller<HttpEntity, ?> unmarshaller = Jackson.unmarshaller(apiRequest.getResponseClass(), apiRequest.getResponseType());
            return (CompletionStage<Object>) unmarshaller.unmarshal(httpResponse.entity(),materializer);
        }else{
            ApiSuccess value = new ApiSuccess(apiRequest.getClass(), httpResponse.entity());
            return CompletableFuture.completedFuture(value);
        }
    }

    private  CompletionStage<Object> handleFailure(HttpResponse httpResponse) {
        Unmarshaller<HttpEntity, ErrorMessage> unmarshaller = Jackson.unmarshaller(ErrorMessage.class);
        return  unmarshaller.unmarshal(httpResponse.entity(),materializer).<Object>thenApplyAsync(csr ->
                new ApiError(apiRequest.getClass(), httpResponse.status(), csr.message)
        ).exceptionally(throwable -> new SerializationException(throwable) );
    }

    private CompletionStage<Either<SerializationException,String>> marshall(Object object){
        try {
            String jsonString = Jackson.getDefaultObjectMapper().writeValueAsString(object);
            return CompletableFuture.completedFuture(new Right(jsonString));
        } catch (JsonProcessingException e) {
            return CompletableFuture.completedFuture(new Left(new SerializationException(e)));
        }

    }
    private CompletionStage<Either<SerializationException, HttpResponse>> executeRequest(Either<SerializationException, String> marshallResult, HttpRequest request) {
        if(marshallResult.isRight()){
            String requestEntity = marshallResult.right().get();
            CompletionStage<HttpResponse> rsp = Http.get(as).singleRequest(request.withEntity(ContentTypes.APPLICATION_JSON, requestEntity), materializer);
            return rsp.thenApply(rsp1 -> new Right(rsp1));
        }
        return CompletableFuture.completedFuture( new Left( marshallResult.left().get()));
    }

    private static class ErrorMessage{
        public String message;
    }
}
