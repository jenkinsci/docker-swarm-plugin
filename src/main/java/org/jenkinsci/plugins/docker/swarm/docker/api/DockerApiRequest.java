package org.jenkinsci.plugins.docker.swarm.docker.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiError;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiException;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiSuccess;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.SerializationException;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.Jackson;

import java.io.IOException;

public class DockerApiRequest {
    private final ApiRequest apiRequest;
    private  static final OkHttpClient client = new OkHttpClient();

    public DockerApiRequest(ApiRequest request) {
        this.apiRequest = request;
    }

    public Object execute(){
        try {
            Request apiCall = apiRequest.toOkHttpRequest();
            Response  response = client.newCall(apiCall).execute();
            return response.isSuccessful()? handleSuccess(response): handleFailure(response);
        } catch (JsonProcessingException e) {
            return new SerializationException(e);
        } catch (IOException e) {
            return new ApiException(apiRequest.getClass(),e);
        }
    }

    private Object handleSuccess(Response response) throws IOException {
        if(apiRequest.getResponseClass() != null){
            return Jackson.fromJSON(response.body().string(), apiRequest.getResponseClass(), apiRequest.getResponseType());
        }else{
            return new ApiSuccess(apiRequest.getClass(), response);
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


    private static class ErrorMessage{
        public String message;
    }
}
