package suryagaddipati.jenkinsdockerslaves.docker.api.request;

import akka.http.javadsl.model.HttpMethod;
import akka.http.javadsl.model.HttpRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import suryagaddipati.jenkinsdockerslaves.DockerSlaveConfiguration;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.CreateServiceResponse;

public  abstract   class ApiRequest {

    @JsonIgnore
    private final HttpMethod method;
    @JsonIgnore
    private final String url;
    @JsonIgnore
    private Class<CreateServiceResponse> responseClass;

    public ApiRequest(HttpMethod method, String url, Class<CreateServiceResponse> responseClass) {
        this.responseClass = responseClass;
        final DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
        this.method = method;
        this.url = configuration.getDockerUri()+ url;
    }
    public ApiRequest(HttpMethod method, String url){
       this(method,url,null) ;
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
}
