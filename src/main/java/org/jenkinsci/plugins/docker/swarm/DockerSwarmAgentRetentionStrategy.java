package org.jenkinsci.plugins.docker.swarm;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.durabletask.executors.ContinuableExecutable;
import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.ExecutorListener;
import hudson.model.Queue;
import hudson.slaves.RetentionStrategy;

public class DockerSwarmAgentRetentionStrategy extends RetentionStrategy<DockerSwarmComputer>
        implements ExecutorListener {
    private static final Logger LOGGER = Logger.getLogger(DockerSwarmAgentRetentionStrategy.class.getName());

    private transient volatile boolean terminating;

    private boolean isTaskAccepted;
    private boolean isTaskCompleted;
    private long timeout; // in ms

    @DataBoundConstructor
    public DockerSwarmAgentRetentionStrategy(int timeout /* in minutes */) {
        this.timeout = MINUTES.toMillis(timeout);
    }

    public long getTimeout() {
        return timeout;
    }

    @Override
    public long check(@Nonnull DockerSwarmComputer c) {
        if (c.isIdle() && c.isOnline()) {
            final long connectTime = System.currentTimeMillis() - c.getConnectTime();
            final long onlineTime = System.currentTimeMillis() - c.getOnlineTime();
            final long idleTime = System.currentTimeMillis() - c.getIdleStartMilliseconds();
            final boolean isTimeout = connectTime > timeout && onlineTime > timeout && idleTime > timeout;
            if (isTimeout && (!isTaskAccepted || isTaskCompleted)) {
                LOGGER.log(Level.INFO, "Disconnecting due to idle {0}", c.getName());
                done(c);
            }
        }
        // Return one because we want to check every minute if idle.
        return 1;
    }

    @Override
    public void start(DockerSwarmComputer c) {
        c.connect(true);
    }

    @Override
    public void taskAccepted(Executor executor, Queue.Task task) {
        this.isTaskAccepted = true;
        getLogger(executor).println("Task accepted: " + task.getFullDisplayName());
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        this.isTaskCompleted = true;
        getLogger(executor).println("Task completed: " + task.getFullDisplayName() + " (" + durationMS + " ms)");
        done(executor);
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        taskCompleted(executor, task, durationMS);
    }

    private PrintStream getLogger(Executor executor) {
        final DockerSwarmComputer c = (DockerSwarmComputer) executor.getOwner();
        return c.getListener().getLogger();
    }

    private void done(Executor executor) {
        final DockerSwarmComputer c = (DockerSwarmComputer) executor.getOwner();
        Queue.Executable exec = executor.getCurrentExecutable();
        if (exec instanceof ContinuableExecutable && ((ContinuableExecutable) exec).willContinue()) {
            LOGGER.log(Level.FINE, "not terminating {0} because {1} says it will be continued",
                    new Object[] { c.getName(), exec });
            return;
        }
        LOGGER.log(Level.FINE, "terminating {0} since {1} seems to be finished", new Object[] { c.getName(), exec });
        done(c);
    }

    private synchronized void done(final DockerSwarmComputer c) {
        c.setAcceptingTasks(false); // just in case
        if (terminating) {
            return;
        }
        terminating = true;
        Computer.threadPoolForRemoting.submit(() -> {
            Queue.withLock(() -> {
                DockerSwarmAgent node = c.getNode();
                if (node != null) {
                    try {
                        node.terminate();
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to terminate " + c.getName(), e);
                    }
                }
            });
        });
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<RetentionStrategy<?>> {
        @Override
        public String getDisplayName() {
            return "Use container only once";
        }
    }

}
