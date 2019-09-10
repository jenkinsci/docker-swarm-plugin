package org.jenkinsci.plugins.docker.swarm;

public class Bytes {
    public static int GB(final int i) {
        return i * 1024 * 1024 * 1024;
    }

    public static int MB(final int i) {
        return i * 1024 * 1024;
    }

    public static long toMB(final long i) {
        return (i / 1024) / 1024;
    }
}
