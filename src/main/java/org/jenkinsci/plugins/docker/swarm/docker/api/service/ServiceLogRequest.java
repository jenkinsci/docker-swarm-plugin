package org.jenkinsci.plugins.docker.swarm.docker.api.service;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;

import java.io.IOException;

public class ServiceLogRequest extends ApiRequest {

    public ServiceLogRequest(String id) throws IOException {
        super(HttpMethod.GET, "/services/" + id + "/logs?follow=true&stdout=true&stderr=true");
    }
}
