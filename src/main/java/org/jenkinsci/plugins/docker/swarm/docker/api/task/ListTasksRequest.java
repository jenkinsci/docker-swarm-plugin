
package org.jenkinsci.plugins.docker.swarm.docker.api.task;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListTasksRequest extends ApiRequest {

    public ListTasksRequest() {
        super(HttpMethod.GET, "/tasks", Task.class, ResponseType.LIST);
    }
    public ListTasksRequest(String dockerSwarmApiUrl, String url) {
        super(HttpMethod.GET, dockerSwarmApiUrl, url, Task.class, ResponseType.LIST);
    }


    public  ListTasksRequest(String dockerApiUrl, String filterKey, String filterValue){
        this(dockerApiUrl, "/tasks?filters="+encodeJsonFilter(filterKey,filterValue));
    }
}
