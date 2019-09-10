
package org.jenkinsci.plugins.docker.swarm.docker.api.service;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListServicesRequest extends ApiRequest {

    public ListServicesRequest(String url) throws IOException {
        super(HttpMethod.GET, url, ScheduledService.class, ResponseType.LIST);
    }

    public ListServicesRequest(String dockerApiUrl, String url) throws IOException {
        super(HttpMethod.GET, dockerApiUrl, url, ScheduledService.class, ResponseType.LIST, null);
    }

    public ListServicesRequest() throws IOException {
        this("/services");
    }

    public ListServicesRequest(String dockerApiUrl, String filterKey, String filterValue) throws IOException {
        this(dockerApiUrl, "/services?filters=" + encodeJsonFilter(filterKey, filterValue));
    }
}
