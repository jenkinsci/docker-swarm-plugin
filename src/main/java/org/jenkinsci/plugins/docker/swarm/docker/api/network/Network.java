package org.jenkinsci.plugins.docker.swarm.docker.api.network;

public class Network {
    public Network() {
    }

    public Network(String target) {
        Target = target;
    }

    public String Target;
}
