package org.jenkinsci.plugins.docker.swarm.docker.api.service;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;

public class DeleteServiceRequest extends ApiRequest {
    public DeleteServiceRequest(String swarmName, String serviceName) throws IOException {
        super(swarmName, HttpMethod.DELETE, "/services/" + serviceName);
    }
}
