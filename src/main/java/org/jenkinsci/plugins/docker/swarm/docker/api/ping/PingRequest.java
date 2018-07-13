package org.jenkinsci.plugins.docker.swarm.docker.api.ping;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;

public class PingRequest extends ApiRequest {
    public PingRequest(String dockerApiUrl) {
        super(HttpMethod.GET, dockerApiUrl, "/_ping",null,null, null);

    }
}
