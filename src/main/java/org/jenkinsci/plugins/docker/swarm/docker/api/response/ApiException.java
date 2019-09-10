package org.jenkinsci.plugins.docker.swarm.docker.api.response;

public class ApiException {
    private Class<?> requestClass;
    private Throwable cause;

    public Class<?> getRequestClass() {
        return requestClass;
    }

    public Throwable getCause() {
        return cause;
    }

    public ApiException(Class<?> requestClass, Throwable cause) {
        this.requestClass = requestClass;
        this.cause = cause;
    }
}
