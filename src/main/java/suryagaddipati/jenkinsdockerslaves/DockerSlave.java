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

import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.EphemeralNode;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;

import java.io.IOException;
import java.util.Collections;

/**
 * An ${@link EphemeralNode} using docker containers to host the build processes.
 * Slave is dedicated to a specific ${@link Job}, and even better to a specific build, but when this class
 * is created the build does not yet exists due to Jenkins lifecycle.
 *
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DockerSlave extends AbstractCloudSlave implements EphemeralNode {

    private final Job job;

    public DockerSlave(Job job, String labelString) throws Descriptor.FormException, IOException {
        // TODO would be better to get notified when the build start, and get the actual build ID. But can't find the API for that
        super(labelString, "Container slave for building " + job.getFullName()+job.getNextBuildNumber(),
                "/home/jenkins", 1, Mode.EXCLUSIVE, labelString,
                new DockerComputerLauncher(),
                RetentionStrategy.NOOP,
                Collections.<NodeProperty<?>>emptyList());
        this.job = job;
    }

    public DockerComputer createComputer() {
        return new DockerComputer(this, job);
    }

    @Override
    protected void _terminate(TaskListener listener) throws IOException, InterruptedException {
       // this.terminate();
    }


    @Override
    public Node asNode() {
        return this;
    }

    @Override
    public DockerComputer getComputer() {
        return (DockerComputer) super.getComputer();
    }



}
