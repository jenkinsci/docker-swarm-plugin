package org.jenkinsci.plugins.docker.swarm.docker.api.service;

import java.io.IOException;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;

public class ServiceLogRequest extends ApiRequest {

    public ServiceLogRequest(String swarmName, String id) throws IOException {
        super(swarmName, HttpMethod.GET, "/services/" + id + "/logs?follow=true&stdout=true&stderr=true");
    }
}
