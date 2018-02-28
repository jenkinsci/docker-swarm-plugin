package org.jenkinsci.plugins.docker.swarm.docker.api;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpMethods;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.ActorMaterializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiError;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiException;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiSuccess;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.SerializationException;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.Jackson;
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class DockerApiRequest {
    private final ActorSystem as;
    private final ApiRequest apiRequest;
    private final ActorMaterializer materializer;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;


    public DockerApiRequest( ActorSystem as, ApiRequest request) {
        this.as = as;
        this.materializer = ActorMaterializer.create(as);
        this.apiRequest = request;
        this.client =  new OkHttpClient();
    }

    public CompletionStage<Object> execute(){
        Object result = null;
        try {
            String jsonString = Jackson.getDefaultObjectMapper().writeValueAsString(apiRequest.getEntity());
            HttpRequest httpRequest = apiRequest.getHttpRequest();
            RequestBody body = RequestBody.create(JSON, jsonString);
            String method = httpRequest.method().name();
            Request apiCall = new Request.Builder()
                    .url(httpRequest.getUri().toString())
                    .method(method, method.equals("GET")?null:body)
                    .build();
            Response  response = client.newCall(apiCall).execute();
            if(response.isSuccessful()){
                result = handleSuccess(response);
            }else {
                result = handleFailure(response);
            }

        } catch (JsonProcessingException e) {
            result = new SerializationException(e);
        } catch (IOException e) {
            e.printStackTrace();
            result  = new ApiException(apiRequest.getClass(),e);
        }
        return CompletableFuture.completedFuture(result);
//        return marshall(apiRequest.getEntity())
//                .thenCompose( marshallResult -> executeRequest( marshallResult, apiRequest.getHttpRequest()))
//                .thenCompose( httpResponse -> marshallResponse(httpResponse))
//                .exceptionally(ex -> new ApiException(apiRequest.getClass(),ex));
    }

    private Object handleSuccess(Response response) throws IOException {
        if(apiRequest.getResponseClass() != null){
            return Jackson.fromJSON(response.body().string(), apiRequest.getResponseClass(), apiRequest.getResponseType());
        }else{
            return new ApiSuccess(apiRequest.getClass(), null);
        }
    }

    private Object handleFailure(Response response) throws IOException {
        Object result;
        if(response.code() == 500 ) {
            result = new ApiError(apiRequest.getClass(), response.code(), response.message()) ;
        }else{
            String responseBody = response.body().string();
            ErrorMessage errorMessage = Jackson.fromJSON(responseBody, ErrorMessage.class);
            result = new ApiError(apiRequest.getClass(), response.code(), errorMessage.message) ;
        }
        return result;
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
        if(httpResponse.status().intValue() == 500 ){
            return CompletableFuture.completedFuture(new ApiError(apiRequest.getClass(), httpResponse.status().intValue(), httpResponse.entity().toString()));
        }
        Unmarshaller<HttpEntity, ErrorMessage> unmarshaller = Jackson.unmarshaller(ErrorMessage.class);
        return  unmarshaller.unmarshal(httpResponse.entity(),materializer).<Object>thenApplyAsync(csr ->
                new ApiError(apiRequest.getClass(), httpResponse.status().intValue(), csr.message)
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
    private CompletionStage<Either<SerializationException, HttpResponse>> executeRequest(Either<SerializationException, String> marshallResult, HttpRequest request){
        if(marshallResult.isRight()){
            String requestEntity = marshallResult.right().get();
            if(request.method().equals(HttpMethods.POST)){
                RequestBody body = RequestBody.create(JSON, requestEntity);
                Request apiCall = new Request.Builder()
                        .url(request.getUri().toString())
                        .method(request.method().name(), body)
                        .build();
                try {
                    Response  response = client.newCall(apiCall).execute();
                    return CompletableFuture.completedFuture(new Right(response.body().string()));
                } catch (IOException e) {
                   throw new RuntimeException(e) ;
                }
            }else{
                CompletionStage<HttpResponse> rsp = Http.get(as).singleRequest(request.withEntity(ContentTypes.APPLICATION_JSON, requestEntity), materializer);
                return rsp.thenApply(rsp1 -> new Right(rsp1));
            }

        }
        return CompletableFuture.completedFuture( new Left( marshallResult.left().get()));
    }

    private static class ErrorMessage{
        public String message;
    }
}
