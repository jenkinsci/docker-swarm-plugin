
package org.jenkinsci.plugins.docker.swarm.docker.api.task;

import akka.http.javadsl.model.HttpMethods;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListTasksRequest extends ApiRequest {

    public ListTasksRequest() {
        super(HttpMethods.GET, "/tasks", Task.class, ResponseType.LIST);
    }
    public ListTasksRequest(String dockerSwarmApiUrl, String url) {
        super(HttpMethods.GET, dockerSwarmApiUrl, url, Task.class, ResponseType.LIST);
    }


    public  ListTasksRequest(String dockerApiUrl, String filterKey, String filterValue){
        this(dockerApiUrl, "/tasks?filters="+encodeJsonFilter(filterKey,filterValue));
    }
}
