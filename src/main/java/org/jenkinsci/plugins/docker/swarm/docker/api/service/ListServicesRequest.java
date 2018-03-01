
package org.jenkinsci.plugins.docker.swarm.docker.api.service;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListServicesRequest extends ApiRequest {

    public ListServicesRequest(String url) {
        super(HttpMethod.GET, url, ScheduledService.class, ResponseType.LIST);
    }

    public ListServicesRequest(String dockerApiUrl, String url) {
        super(HttpMethod.GET, dockerApiUrl, url, ScheduledService.class, ResponseType.LIST);
    }
    public ListServicesRequest() {
        this("/services");
    }
    public  ListServicesRequest(String dockerApiUrl, String filterKey, String filterValue){
       this(dockerApiUrl, "/services?filters="+encodeJsonFilter(filterKey,filterValue));
    }
}
