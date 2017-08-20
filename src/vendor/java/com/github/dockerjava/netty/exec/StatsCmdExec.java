package com.github.dockerjava.netty.exec;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.DockerClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.dockerjava.netty.WebTarget;

public class StatsCmdExec implements StatsCmd.Exec {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsCmdExec.class);
    private WebTarget webTarget;

    public StatsCmdExec(WebTarget webTarget, DockerClientConfig dockerClientConfig) {
        this.webTarget = webTarget;
    }

    @Override
    public Statistics exec(StatsCmd command) {
        return webTarget.path("/containers/{id}/stats").queryParam("stream", "false").request().get(new TypeReference<Statistics>() {
        });
    }
}
