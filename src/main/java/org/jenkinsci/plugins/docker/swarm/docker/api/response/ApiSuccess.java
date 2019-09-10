package org.jenkinsci.plugins.docker.swarm.docker.api.response;

import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;

import okhttp3.Response;

public class ApiSuccess {

    private Class<? extends ApiRequest> requestClass;
    private Response response;

    public ApiSuccess(Class<? extends ApiRequest> requestClass, Response response) {
        this.requestClass = requestClass;
        this.response = response;
    }

    public Class<? extends ApiRequest> getRequestClass() {
        return requestClass;
    }

    public Response getResponse() {
        return response;
    }

}
