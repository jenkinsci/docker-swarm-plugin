
package org.jenkinsci.plugins.docker.swarm.docker.api.secrets;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ScheduledService;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListSecretsRequest extends ApiRequest {

    public ListSecretsRequest(String url) throws IOException {
        super(HttpMethod.GET, url, Secret.class, ResponseType.LIST);
    }

    public ListSecretsRequest(String dockerApiUrl, String url) throws IOException {
        super(HttpMethod.GET, dockerApiUrl, url, ScheduledService.class, ResponseType.LIST, null);
    }

    public ListSecretsRequest() throws IOException {
        this("/secrets");
    }

    public ListSecretsRequest(String dockerApiUrl, String filterKey, String filterValue) throws IOException {
        this(dockerApiUrl, "/secrets?filters=" + encodeJsonFilter(filterKey, filterValue));
    }
}
