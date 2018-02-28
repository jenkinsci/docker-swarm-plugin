
package org.jenkinsci.plugins.docker.swarm;

import com.google.common.collect.Iterables;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.OfflineCause;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class DockerSwarmComputer extends AbstractCloudComputer<DockerSwarmAgent> {

    public DockerSwarmComputer(final DockerSwarmAgent dockerSwarmAgent) {
        super(dockerSwarmAgent);
    }

    public Queue.Executable getCurrentBuild() {
        if (!Iterables.isEmpty(getExecutors())) {
            final Executor exec = getExecutors().get(0);
            return exec.getCurrentExecutable() == null ? null : exec.getCurrentExecutable();
        }
        return null;
    }


    @Override
    public Map<String, Object> getMonitorData() {
        return new HashMap<>(); //no monitoring needed as this is a shortlived computer.
    }

    @Override
    public void recordTermination() {
        //no need to record termination
    }

    @Override
    public boolean isLaunchSupported() {
        return false;
    }

    @Override
    public boolean isManualLaunchAllowed() {
        return false;
    }

    @Override
    public Future<?> disconnect(OfflineCause cause) {
        return CompletableFuture.completedFuture(true);
    }



    public String getVolumeName() {
        return getName().split("-")[1];
    }
}
