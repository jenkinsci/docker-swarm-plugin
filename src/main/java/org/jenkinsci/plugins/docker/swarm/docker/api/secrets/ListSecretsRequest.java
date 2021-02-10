
package org.jenkinsci.plugins.docker.swarm.docker.api.secrets;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ScheduledService;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListSecretsRequest extends ApiRequest {
    public ListSecretsRequest(String swarmName, String url) throws IOException {
        super(swarmName, HttpMethod.GET, url, ScheduledService.class, ResponseType.LIST, null);
    }

    public ListSecretsRequest(String swarmName) throws IOException {
        this(swarmName, "/secrets");
    }

    public ListSecretsRequest(String swarmName, String filterKey, String filterValue) throws IOException {
        this(swarmName, "/secrets?filters=" + encodeJsonFilter(filterKey, filterValue));
    }
}
