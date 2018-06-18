package org.jenkinsci.plugins.docker.swarm.docker.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import hudson.model.Api;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmCloud;
import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.error.ErrorMessage;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiError;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiException;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiSuccess;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.SerializationException;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.Jackson;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ApiRequest {
    @JsonIgnore
    private final HttpMethod method;
    @JsonIgnore
    private final String url;
    @JsonIgnore
    private static String credentialsId;
    @JsonIgnore
    private Class<?> responseClass;
    @JsonIgnore
    private ResponseType responseType;
    @JsonIgnore
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    @JsonIgnore
    private static OkHttpClient client;
    @JsonIgnore
    private static final Logger LOGGER = Logger.getLogger(ApiRequest.class.getName());

    public ApiRequest(HttpMethod method, String dockerApiUrl, String url, Class<?> responseClass , ResponseType responseType) throws IOException {
        this.responseClass = responseClass;
        this.responseType = responseType;
        this.method = method;
        this.url = dockerApiUrl+url;
        String dockerCredentialsId = null;
        if (DockerSwarmCloud.get().getDockerHost() != null) {
            dockerCredentialsId = DockerSwarmCloud.get().getDockerHost().getCredentialsId();
        }
        if (client == null || (dockerCredentialsId != null && !dockerCredentialsId.equals(ApiRequest.credentialsId))) {
            if (dockerCredentialsId != null) {
                client = new OkHttpClient.Builder().sslSocketFactory(DockerSwarmCloud.get().getSSLContext().getSocketFactory()).build();
            } else {
                client = new OkHttpClient();
            }
            ApiRequest.credentialsId = dockerCredentialsId;
        }
    }
    public ApiRequest(HttpMethod method, String url, Class<?> responseClass , ResponseType responseType) throws IOException  {
        this(method, DockerSwarmCloud.get().getDockerSwarmApiUrl(),url,responseClass,responseType);
    }
    public ApiRequest(HttpMethod method, String url) throws IOException  {
       this(method,url,null,null) ;
    }

    protected static String encodeJsonFilter(String filterKey, String filterValue) {
        Map<Object,Object> filter = new HashMap<>();
        filter.put(filterKey,new String[]{filterValue});
        try {
            return URLEncoder.encode( Jackson.toJson(filter),"UTF-8");
        } catch (UnsupportedEncodingException e) {
           throw new RuntimeException(e);
        }
    }
    private String getUrl() {
        return url;
    }

    private HttpMethod getMethod() {
        return method;
    }

    public Class<?> getResponseClass(){
        return responseClass;
    }
    public Object getEntity() {
        return this;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public String toJsonString() {
        return Jackson.toJson(getEntity());
    }

    private Request toOkHttpRequest() {
        String jsonString = toJsonString();
        LOGGER.log(Level.FINE,"JSON Request: {0}, {1}", new Object[]{getUrl(), jsonString});
        RequestBody body = RequestBody.create(JSON, jsonString);
        String method = getMethod().name();
        return new Request.Builder()
                .url(getUrl())
                .method(method, method.equals("GET")?null:body)
                .build();
    }

    public Object execute(){
        try {
            Request apiCall = toOkHttpRequest();
            Response response = client.newCall(apiCall).execute();
            return response.isSuccessful()? handleSuccess(response): handleFailure(response);
        } catch (JsonProcessingException e) {
            LOGGER.log(Level.WARNING,"Serialisation exception", e);
            return new SerializationException(e);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING,"Execute IO exception", e);
            return new ApiException(responseClass,e);
        }
    }
    private Object handleSuccess(Response response) throws IOException {
        if(getResponseClass() != null){
            String responseBody = response.body().string();
            LOGGER.log(Level.FINE,"API Request response success: {0}", responseBody);
            return Jackson.fromJSON(responseBody, getResponseClass(), getResponseType());
        }else{
            LOGGER.log(Level.FINE,"API Request response success");
            return new ApiSuccess(getClass(), response);
        }
    }

    private Object handleFailure(Response response) throws IOException {
        Object result;
        if(response.code() == 500 ) {
            LOGGER.log(Level.WARNING,"API Request response status 500. Message: {0}", response.message());
            result = new ApiError(getClass(), response.code(), response.message()) ;
        }else{
            String responseBody = response.body().string();
            LOGGER.log(Level.WARNING,"API Request response fail: {0}", responseBody);
            ErrorMessage errorMessage = Jackson.fromJSON(responseBody, ErrorMessage.class);
            result = new ApiError(getClass(), response.code(), errorMessage.message) ;
        }
        return result;
    }
}
