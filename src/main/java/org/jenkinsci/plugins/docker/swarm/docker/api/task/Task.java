package org.jenkinsci.plugins.docker.swarm.docker.api.task;

import org.jenkinsci.plugins.docker.swarm.Bytes;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ScheduledService;

public class Task {
    public TaskTemplate Spec;
    public String NodeID;
    public String ServiceID;
    public Status Status;

    public String getServiceID() {
        return ServiceID;
    }

    public long getReservedCpus() {
        return Spec.Resources.Reservations.NanoCPUs / 1000000000;
    }

    public long getReservedMemory() {
        return Bytes.toMB(Spec.Resources.Reservations.MemoryBytes);
    }

    public ScheduledService service;

    public boolean isComplete() {
        return Status.isComplete();
    }
}
