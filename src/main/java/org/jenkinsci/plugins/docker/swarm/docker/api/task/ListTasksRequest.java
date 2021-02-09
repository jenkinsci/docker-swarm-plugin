
package org.jenkinsci.plugins.docker.swarm.docker.api.task;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListTasksRequest extends ApiRequest {

    public ListTasksRequest(String swarmName) throws IOException {
        super(swarmName, HttpMethod.GET, "/tasks", Task.class, ResponseType.LIST);
    }

    public ListTasksRequest(String swarmName, String url) throws IOException {
        super(swarmName, HttpMethod.GET, url, Task.class, ResponseType.LIST, null);
    }

    public ListTasksRequest(String swarmName, String filterKey, String filterValue) throws IOException {
        this(swarmName, "/tasks?filters=" + encodeJsonFilter(filterKey, filterValue));
    }
}
