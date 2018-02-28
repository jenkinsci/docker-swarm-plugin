package org.jenkinsci.plugins.docker.swarm.docker.api.response;

import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;

public class ApiError {
    public ApiError(Class<? extends ApiRequest> requestClass, int statusCode, String message) {
        this.requestClass = requestClass;
        this.statusCode = statusCode;
        this.message = message;
    }

    public Class<?> getRequestClass() {
        return requestClass;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    private Class<?> requestClass;
    private int statusCode;
    private String message;
}
