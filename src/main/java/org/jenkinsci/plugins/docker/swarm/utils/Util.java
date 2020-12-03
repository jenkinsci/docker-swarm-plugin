package org.jenkinsci.plugins.docker.swarm.utils;

import hudson.model.Queue;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Util extends hudson.Util {

    private static AtomicInteger counter = new AtomicInteger(1);

    public static <T> Collector<T, ?, T> singletonCollector() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.size() != 1) {
                return null;
            }
            return list.get(0);
        });
    }

    public static int getUniqueNumber() {
        return counter.incrementAndGet();
    }

    public static String codenamizeTask(Queue.Task task) {
        String label = fixEmptyAndTrim(task.getAssignedLabel().getName());
        String nameHash = Long.toUnsignedString(task.getFullDisplayName().hashCode(), Character.MAX_RADIX);
        return label + "-" + getUniqueNumber() + "-" + nameHash;
    }
    
}
