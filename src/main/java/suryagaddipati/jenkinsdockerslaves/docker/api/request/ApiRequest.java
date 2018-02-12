package suryagaddipati.jenkinsdockerslaves.docker.api.request;

import akka.http.javadsl.model.HttpMethod;
import akka.http.javadsl.model.HttpRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import suryagaddipati.jenkinsdockerslaves.DockerSwarmCloud;
import suryagaddipati.jenkinsdockerslaves.docker.marshalling.Jackson;
import suryagaddipati.jenkinsdockerslaves.docker.marshalling.ResponseType;

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

    public ApiRequest(HttpMethod method, String dockerApiUrl, String url, Class<?> responseClass , ResponseType responseType) {
        this.responseClass = responseClass;
        this.responseType = responseType;
        this.method = method;
        this.url = dockerApiUrl+url;
    }
    public ApiRequest(HttpMethod method, String url, Class<?> responseClass , ResponseType responseType) {
        this(method,DockerSwarmCloud.get().getDockerSwarmApiUrl(),url,responseClass,responseType);
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

    public HttpRequest getHttpRequest() {
        return HttpRequest.create(url).withMethod(method);
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

}
