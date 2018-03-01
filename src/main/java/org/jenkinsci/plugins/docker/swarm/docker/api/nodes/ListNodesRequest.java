
package org.jenkinsci.plugins.docker.swarm.docker.api.nodes;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListNodesRequest extends ApiRequest {

    public ListNodesRequest() {
        super(HttpMethod.GET, "/nodes", Node.class, ResponseType.LIST);
    }
    public ListNodesRequest(String dockerSwarmApiUrl) {
        super(HttpMethod.GET, dockerSwarmApiUrl, "/nodes", Node.class, ResponseType.LIST);
    }
}
