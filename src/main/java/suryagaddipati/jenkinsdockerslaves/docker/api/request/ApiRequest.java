package suryagaddipati.jenkinsdockerslaves.docker.api.request;

import akka.http.javadsl.model.HttpMethod;
import akka.http.javadsl.model.HttpRequest;
import suryagaddipati.jenkinsdockerslaves.DockerSlaveConfiguration;

public  abstract   class ApiRequest {

    private final HttpMethod method;
    private final String url;

    public Object getEntity() {
        return entity;
    }

    private final Object  entity;

    public ApiRequest(HttpMethod method, String url) {
        this(method,url,null);
    }

    public ApiRequest(HttpMethod method, String url, Object entity) {
        final DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
        this.method = method;
        this.url = configuration.getDockerUri()+ url;
        this.entity = entity;
    }

    public HttpRequest getHttpRequest() {
        return HttpRequest.create(url).withMethod(method);
    }


    public abstract String getName();

    public abstract  Class<?> getResponseClass();
}
