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
import com.google.common.collect.Iterables;
import hudson.model.AbstractProject;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.slaves.AbstractCloudComputer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerComputer extends AbstractCloudComputer<DockerSlave> {

    private String containerId;
    private String swarmNodeName;
    private Date launchTime;
    private static final Logger LOGGER = Logger.getLogger(DockerComputer .class.getName());


    public DockerComputer(DockerSlave dockerSlave)  {
        super(dockerSlave);
    }

    @Override
    public void recordTermination() {
        if (isAcceptingTasks()) {
            super.recordTermination();
        }
    }


    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        super.taskCompleted(executor, task, durationMS);
        terminateNode(task);
    }

    private void terminateNode(Queue.Task task)  {
        if(!(task instanceof Run)){// workflow doesn't use Run so this is needed as a backup incase RunListner doesn't catch it.
            try {
                new ContainerCleanupListener().terminate(this,System.out);
            } catch (IOException|InterruptedException e) {
               LOGGER.log(Level.INFO,"Failed to Cleanup node "+ getName(),e);
            }
        }
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        super.taskCompletedWithProblems(executor, task, durationMS, problems);
        terminateNode(task);
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public void setNodeName(String nodeName) {
        this.swarmNodeName = nodeName;
    }

    public String getSwarmNodeName() {
        return swarmNodeName;
    }

    public Queue.Executable getCurrentBuild(){
        if (!Iterables.isEmpty(getExecutors())){
            Executor exec = getExecutors().get(0);
            return exec.getCurrentExecutable()==null? null: exec.getCurrentExecutable();
        }
        return null;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setLaunchTime(Date launchTime) {
        this.launchTime = launchTime;
    }
    public Date getLaunchTime() {
        return launchTime;
    }

    public  void pause() throws IOException {
        try(DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            dockerClient.pauseContainerCmd(containerId).exec();
        }
    }
    public  void unpause() throws IOException {
        try(DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
             dockerClient.unpauseContainerCmd(containerId).exec();
        }
    }

    public boolean isPausable() throws IOException {
        try(DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
            return !container.getState().getPaused();
        }
    }

    public boolean isUnPausable() throws IOException {
        try(DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
            return container.getState().getPaused();
        }
    }

    public void cleanupDockerContainer(PrintStream logger) throws IOException {

        if(isUnPausable()){
           unpause();
        }
        DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
        try( DockerClient dockerClient = configuration.newDockerClient()){
            if (containerId != null){
                DockerApiHelpers.executeSliently(() -> dockerClient.killContainerCmd(containerId).exec());
                DockerApiHelpers.executeSlientlyWithLogging(()->removeContainer(logger, containerId, dockerClient), LOGGER,"Failed to cleanup container : " +getName());
            }
        }
    }
    private void removeContainer(PrintStream logger, String containerId, DockerClient dockerClient) {
        DockerApiHelpers.executeWithRetryOnError(() -> dockerClient.removeContainerCmd(containerId).exec() );
        logger.println("Removed Container " + containerId);
    }
}
