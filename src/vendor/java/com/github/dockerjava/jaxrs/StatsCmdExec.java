package com.github.dockerjava.jaxrs;

import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.DockerClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class StatsCmdExec extends AbstrSyncDockerCmdExec<StatsCmd, Statistics> implements StatsCmd.Exec {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatsCmdExec.class);

    public StatsCmdExec(WebTarget baseResource, DockerClientConfig dockerClientConfig) {
        super(baseResource, dockerClientConfig);
    }

    @Override
    protected Statistics execute(StatsCmd command) {
        WebTarget webResource = getBaseResource().path("/containers/{id}/stats").queryParam("stream", "false").resolveTemplate("id",
                command.getContainerId());

        LOGGER.trace("GET: {}", webResource);
        return webResource.request().accept(MediaType.APPLICATION_JSON).get(Statistics.class);
    }

}
