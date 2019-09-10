package org.jenkinsci.plugins.docker.swarm;

import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Util {
    public static <T> Collector<T, ?, T> singletonCollector() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.size() != 1) {
                return null;
            }
            return list.get(0);
        });
    }
}
