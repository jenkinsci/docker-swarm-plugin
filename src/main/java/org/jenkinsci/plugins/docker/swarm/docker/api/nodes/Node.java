package org.jenkinsci.plugins.docker.swarm.docker.api.nodes;

public class Node {
    public String ID;
    public Spec Spec;
    public Status Status;
    public Description Description;

    public static class Spec {
        public String Availability;
        public String Role;
    }

    public static class Status {
        public String State;
    }

    public static class Description {
        public String Hostname;
        public Resources Resources;

        public static class Resources {
            public Long NanoCPUs;
            public Long MemoryBytes;
        }
    }
}
