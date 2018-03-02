package org.jenkinsci.plugins.docker.swarm.docker.api.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jenkinsci.plugins.docker.swarm.DockerSwarmCloud;
import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.Jackson;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public abstract class ApiRequest {
    @JsonIgnore
    private final HttpMethod method;
    @JsonIgnore
    private final String url;
    @JsonIgnore
    private Class<?> responseClass;
    @JsonIgnore
    private ResponseType responseType;
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public ApiRequest(HttpMethod method, String dockerApiUrl, String url, Class<?> responseClass , ResponseType responseType) {
        this.responseClass = responseClass;
        this.responseType = responseType;
        this.method = method;
        this.url = dockerApiUrl+url;
    }
    public ApiRequest(HttpMethod method, String url, Class<?> responseClass , ResponseType responseType) {
        this(method, DockerSwarmCloud.get().getDockerSwarmApiUrl(),url,responseClass,responseType);
    }
    public ApiRequest(HttpMethod method, String url){
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

    public Request toOkHttpRequest() throws JsonProcessingException {
        String jsonString = Jackson.toJson(getEntity());
        RequestBody body = RequestBody.create(JSON, jsonString);
        String method = getMethod().name();
        return new Request.Builder()
                .url(getUrl())
                .method(method, method.equals("GET")?null:body)
                .build();
    }
}
