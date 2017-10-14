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

import akka.actor.ActorRef;
import hudson.model.Descriptor;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.model.labels.LabelAtom;
import hudson.model.queue.CauseOfBlockage;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.EphemeralNode;
import jenkins.model.Jenkins;
import suryagaddipati.jenkinsdockerslaves.docker.DeleteServiceRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class DockerSlave extends AbstractCloudSlave implements EphemeralNode {


    public DockerSlave(final Queue.BuildableItem bi, final String labelString) throws Descriptor.FormException, IOException {
        super(labelString, "Container slave for building " + bi.task.getFullDisplayName(),
                "/home/jenkins", 1, Mode.EXCLUSIVE, labelString,
                new DockerComputerLauncher(bi),
                new DockerSlaveRetentionStrategy(),
                Collections.emptyList());
    }

    public DockerComputer createComputer() {
        return new DockerComputer(this);
    }

    @Override
    protected void _terminate(final TaskListener listener) throws IOException, InterruptedException {
    }

    @Override
    public void terminate() throws InterruptedException, IOException {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        ActorRef agentLauncherRef = swarmPlugin.getActorSystem().actorFor("/user/" + getComputer().getName());
        agentLauncherRef.tell(new DeleteServiceRequest(getComputer().getName()),ActorRef.noSender());
        super.terminate();
    }

    @Override
    public Node asNode() {
        return this;
    }


    @Override
    public CauseOfBlockage canTake(final Queue.BuildableItem item) {
        final Label l = item.getAssignedLabel();
        if (l != null && this.name.equals(l.getName())) {
            return null;
        }
        return super.canTake(item);
    }

    @Override
    public Set<LabelAtom> getAssignedLabels() {
        final TreeSet<LabelAtom> labels = new TreeSet<>();
        labels.add(new LabelAtom(getLabelString()));
        return labels;
    }
}

