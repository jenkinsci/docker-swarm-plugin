package com.github.dockerjava.core.command;

import com.github.dockerjava.api.command.DockerCmdSyncExec;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Statistics;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Container stats
 */
public class StatsCmdImpl extends AbstrDockerCmd<StatsCmd, Statistics> implements StatsCmd {

    private String containerId;

    public StatsCmdImpl(DockerCmdSyncExec<StatsCmd, Statistics> execution, String containerId) {
        super(execution);
        this.containerId = containerId;
    }


    @Override
    public String getContainerId() {
        return containerId;
    }

    @Override
    public StatsCmd withContainerId(String containerId) {
        checkNotNull(containerId, "containerId was not specified");
        this.containerId = containerId;
        return this;
    }

}
