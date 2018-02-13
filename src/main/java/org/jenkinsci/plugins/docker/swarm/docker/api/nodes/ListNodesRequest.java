
package org.jenkinsci.plugins.docker.swarm.docker.api.nodes;

import akka.http.javadsl.model.HttpMethods;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListNodesRequest extends ApiRequest {

    public ListNodesRequest() {
        super(HttpMethods.GET, "/nodes", Node.class, ResponseType.LIST);
    }
    public ListNodesRequest(String dockerSwarmApiUrl) {
        super(HttpMethods.GET, dockerSwarmApiUrl, "/nodes", Node.class, ResponseType.LIST);
    }
}
