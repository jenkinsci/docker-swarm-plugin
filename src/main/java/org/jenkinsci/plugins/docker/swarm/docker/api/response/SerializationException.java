package org.jenkinsci.plugins.docker.swarm.docker.api.response;

public class SerializationException {
    public SerializationException(Throwable cause) {
        this.cause = cause;
    }

    public Throwable cause;

    public Throwable getCause() {
        return cause;
    }
}
