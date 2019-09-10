package org.jenkinsci.plugins.docker.swarm.docker.api.service;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;

public class DeleteServiceRequest extends ApiRequest {
    public DeleteServiceRequest(String serviceName) throws IOException {
        super(HttpMethod.DELETE, "/services/" + serviceName);
    }

    public DeleteServiceRequest(String dockerApiUrl, String serviceName) throws IOException {
        super(HttpMethod.DELETE, dockerApiUrl, "/services/" + serviceName, null, null, null);
    }

}
