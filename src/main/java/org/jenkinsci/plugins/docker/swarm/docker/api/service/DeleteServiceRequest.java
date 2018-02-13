package suryagaddipati.jenkinsdockerslaves.docker.api.service;

import akka.http.javadsl.model.HttpMethods;
import suryagaddipati.jenkinsdockerslaves.docker.api.request.ApiRequest;

public class DeleteServiceRequest extends ApiRequest {
    public DeleteServiceRequest(String serviceName) {
        super(HttpMethods.DELETE,"/services/"+serviceName);
    }
    public DeleteServiceRequest(String dockerApiUrl, String serviceName) {
        super(HttpMethods.DELETE, dockerApiUrl, "/services/"+serviceName,null,null);
    }

}
