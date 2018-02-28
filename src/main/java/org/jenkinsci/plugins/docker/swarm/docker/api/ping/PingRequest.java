package org.jenkinsci.plugins.docker.swarm.docker.api.ping;

import akka.http.javadsl.model.HttpMethods;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;

public class PingRequest extends ApiRequest {
    public PingRequest(String dockerApiUrl) {
        super(HttpMethods.GET, dockerApiUrl, "/_ping",null,null);

    }
}
