/*
 * The MIT License
 *
 *  Copyright (c) 2015, CloudBees, Inc.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package suryagaddipati.jenkinsdockerslaves;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Statistics;
import com.google.common.collect.Iterables;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.slaves.AbstractCloudComputer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static suryagaddipati.jenkinsdockerslaves.ExceptionHandlingHelpers.executeSliently;
import static suryagaddipati.jenkinsdockerslaves.ExceptionHandlingHelpers.executeSlientlyWithLogging;

public class DockerComputer extends AbstractCloudComputer<DockerSlave> {

    private String containerId;
    private String swarmNodeName;


    public DockerComputer(final DockerSlave dockerSlave) {
        super(dockerSlave);
    }


    public void setNodeName(final String nodeName) {
        this.swarmNodeName = nodeName;
    }

    public String getSwarmNodeName() {
        return this.swarmNodeName;
    }

    public Queue.Executable getCurrentBuild() {
        if (!Iterables.isEmpty(getExecutors())) {
            final Executor exec = getExecutors().get(0);
            return exec.getCurrentExecutable() == null ? null : exec.getCurrentExecutable();
        }
        return null;
    }

    public String getContainerId() {
        return this.containerId;
    }

    public void setContainerId(final String containerId) {
        this.containerId = containerId;
    }

    @Override
    public Map<String, Object> getMonitorData() {
        return new HashMap<>(); //no monitoring needed as this is a shortlived computer.
    }


    private void cleanupNode(final PrintStream logger) throws IOException, InterruptedException {
        if (getNode() != null) {
            logger.println("Removing node " + getNode().getDisplayName());
            getNode().terminate();
        }
    }


    private void gatherStats(final DockerClient dockerClient, final Queue.Executable currentExecutable) throws IOException {
        final String containerId = getContainerId();
        if (currentExecutable instanceof Run && ((Run) currentExecutable).getAction(DockerSlaveInfo.class) != null) {
            final Run run = ((Run) currentExecutable);
            final DockerSlaveInfo slaveInfo = ((Run) currentExecutable).getAction(DockerSlaveInfo.class);
            final Statistics stats = dockerClient.statsCmd(containerId).exec();
            slaveInfo.setStats(stats);
            run.save();
        }
    }

    public void collectStatsAndCleanupDockerContainer(final String containerId, final Queue.Executable currentExecutable, final PrintStream logger) throws IOException {

        final DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
        try (DockerClient dockerClient = configuration.newDockerClient()) {
            if (containerId != null) {
                try {
                    final InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
                    executeSlientlyWithLogging(() -> gatherStats(dockerClient, currentExecutable), logger); // No big deal if we can't get stats
                    if (container.getState().getPaused()) {
                        executeSlientlyWithLogging(() -> dockerClient.unpauseContainerCmd(containerId).exec(), logger);
                    }
                    executeSliently(() -> dockerClient.killContainerCmd(containerId).exec());
                    executeSlientlyWithLogging(() -> removeContainer(logger, containerId, dockerClient), logger);
                } catch (final NotFoundException e) {
                    //Ignore if container is already gone
                }

            }
        }
    }

    private void removeContainer(final PrintStream logger, final String containerId, final DockerClient dockerClient) {
        ExceptionHandlingHelpers.executeWithRetryOnError(() -> dockerClient.removeContainerCmd(containerId).withForce(true).exec());
        logger.println("Removed Container " + containerId);
    }

    @Override
    public void recordTermination() {
        //no need to record termination
    }

    public void delete(final Queue.Executable currentExecutable) {
        executeSlientlyWithLogging(() -> collectStatsAndCleanupDockerContainer(getContainerId(), currentExecutable, System.out), System.out); // Maybe be container was created, so attempt to delete it
        executeSlientlyWithLogging(() -> {
            if (getChannel() != null) getChannel().close();
        }, System.out);
        executeSlientlyWithLogging(() -> cleanupNode(System.out), System.out);
    }

    public void delete() {
        this.delete(null);
    }
}
