
package org.jenkinsci.plugins.docker.swarm.docker.api.configs;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ScheduledService;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListConfigsRequest extends ApiRequest {

    public ListConfigsRequest(String swarmName, String url) throws IOException {
        super(swarmName, HttpMethod.GET, url, ScheduledService.class, ResponseType.LIST, null);
    }

    public ListConfigsRequest(String swarmName) throws IOException {
        this(swarmName, "/configs");
    }

    public ListConfigsRequest(String swarmName, String filterKey, String filterValue) throws IOException {
        this(swarmName, "/configs?filters=" + encodeJsonFilter(filterKey, filterValue));
    }
}
