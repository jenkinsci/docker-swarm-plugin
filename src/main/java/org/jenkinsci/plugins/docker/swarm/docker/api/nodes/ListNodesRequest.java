
package org.jenkinsci.plugins.docker.swarm.docker.api.nodes;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ListNodesRequest extends ApiRequest {

    public ListNodesRequest() throws IOException {
        super(HttpMethod.GET, "/nodes", Node.class, ResponseType.LIST);
    }

    public ListNodesRequest(String dockerSwarmApiUrl) throws IOException {
        super(HttpMethod.GET, dockerSwarmApiUrl, "/nodes", Node.class, ResponseType.LIST, null);
    }
}
