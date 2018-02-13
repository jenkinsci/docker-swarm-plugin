
package suryagaddipati.jenkinsdockerslaves.docker.api.nodes;

import akka.http.javadsl.model.HttpMethods;
import suryagaddipati.jenkinsdockerslaves.docker.api.request.ApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.marshalling.ResponseType;

public class ListNodesRequest extends ApiRequest {

    public ListNodesRequest() {
        super(HttpMethods.GET, "/nodes", Node.class, ResponseType.LIST);
    }
    public ListNodesRequest(String dockerSwarmApiUrl) {
        super(HttpMethods.GET, dockerSwarmApiUrl, "/nodes", Node.class, ResponseType.LIST);
    }
}
