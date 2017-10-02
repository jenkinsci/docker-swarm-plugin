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
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.slaves.AbstractCloudComputer;

import java.util.HashMap;
import java.util.Map;

public class DockerComputer extends AbstractCloudComputer<DockerSlave> {

    private String containerId;
    private String swarmNodeName;
    private boolean isConnecting;


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
    public boolean isConnecting() {
        return this.isConnecting;
    }

    public void setConnecting(boolean connecting) {
        isConnecting = connecting;
    }
}
