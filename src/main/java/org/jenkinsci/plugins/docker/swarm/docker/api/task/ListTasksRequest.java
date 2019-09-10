
package org.jenkinsci.plugins.docker.swarm.docker.api.task;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListTasksRequest extends ApiRequest {

    public ListTasksRequest() throws IOException {
        super(HttpMethod.GET, "/tasks", Task.class, ResponseType.LIST);
    }

    public ListTasksRequest(String dockerSwarmApiUrl, String url) throws IOException {
        super(HttpMethod.GET, dockerSwarmApiUrl, url, Task.class, ResponseType.LIST, null);
    }

    public ListTasksRequest(String dockerApiUrl, String filterKey, String filterValue) throws IOException {
        this(dockerApiUrl, "/tasks?filters=" + encodeJsonFilter(filterKey, filterValue));
    }
}
