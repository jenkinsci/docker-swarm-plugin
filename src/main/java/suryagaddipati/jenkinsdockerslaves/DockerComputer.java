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

import com.google.common.collect.Iterables;
import hudson.model.AbstractProject;
import hudson.model.Executor;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.slaves.AbstractCloudComputer;

import java.util.Date;

public class DockerComputer extends AbstractCloudComputer<DockerSlave> {

    private String containerId;
    private String swarmNodeName;
    private Date launchTime;


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
        if(!(task instanceof AbstractProject)){
            new ContainerCleanupListener().terminate(this,System.out);
        }
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        super.taskCompletedWithProblems(executor, task, durationMS, problems);
        if(!(task instanceof AbstractProject)) {
            new ContainerCleanupListener().terminate(this, System.out);
        }
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
}
