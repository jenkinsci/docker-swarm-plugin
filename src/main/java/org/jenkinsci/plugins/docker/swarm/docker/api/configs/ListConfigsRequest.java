
package org.jenkinsci.plugins.docker.swarm.docker.api.configs;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ScheduledService;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

import java.io.IOException;

public class ListConfigsRequest extends ApiRequest {

    public ListConfigsRequest(String url)  throws IOException {
        super(HttpMethod.GET, url, Config.class, ResponseType.LIST);
    }

    public ListConfigsRequest(String dockerApiUrl, String url) throws IOException {
        super(HttpMethod.GET, dockerApiUrl, url, ScheduledService.class, ResponseType.LIST);
    }
    public ListConfigsRequest() throws IOException {
        this("/configs");
    }
    public ListConfigsRequest(String dockerApiUrl, String filterKey, String filterValue) throws IOException{
       this(dockerApiUrl, "/configs?filters="+encodeJsonFilter(filterKey,filterValue));
    }
}
