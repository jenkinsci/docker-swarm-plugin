
package org.jenkinsci.plugins.docker.swarm.docker.api.service;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListServicesRequest extends ApiRequest {
    public ListServicesRequest(String swarmName, String url) throws IOException {
        super(swarmName, HttpMethod.GET, url, ScheduledService.class, ResponseType.LIST, null);
    }

    public ListServicesRequest(String swarmName) throws IOException {
        this(swarmName, "/services");
    }

    public ListServicesRequest(String swarmName, String filterKey, String filterValue) throws IOException {
        this(swarmName, "/services?filters=" + encodeJsonFilter(filterKey, filterValue));
    }
}
