package org.jenkinsci.plugins.docker.swarm.docker.api.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Strings.emptyToNull;

public class EndpointSpec {

    public List<PortConfig> Ports = new ArrayList<>();

    public EndpointSpec() {
        // For deserialization
    }

    public void addPortBind(String publishedPort, String targetPort, String protocol) {
        this.Ports.add(new PortConfig(
                Optional.ofNullable(emptyToNull(publishedPort)).map(Integer::valueOf).orElse(null),
                Integer.valueOf(targetPort),
                Optional.ofNullable(emptyToNull(protocol)).orElse("tcp")));
    }

    public static class PortConfig {

        String Protocol;
        Integer PublishedPort;
        Integer TargetPort;

        public PortConfig() {
            // For deserialization
        }

        public PortConfig(Integer publishedPort, Integer targetPort, String protocol) {
            this.PublishedPort = publishedPort;
            this.TargetPort = targetPort;
            this.Protocol = protocol;
        }
    }
}
