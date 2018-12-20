package org.jenkinsci.plugins.docker.swarm;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.ExecutorListener;
import hudson.model.Queue;
import hudson.slaves.RetentionStrategy;
import org.jenkinsci.plugins.durabletask.executors.ContinuableExecutable;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MINUTES;

public class DockerSwarmAgentRetentionStrategy extends RetentionStrategy<DockerSwarmComputer> implements ExecutorListener {
    private static final Logger LOGGER = Logger.getLogger(DockerSwarmAgentRetentionStrategy.class.getName());

    private int timeout = 1;
    private transient volatile boolean terminating;
    private boolean isTaskCompleted;

    @DataBoundConstructor
    public DockerSwarmAgentRetentionStrategy(int idleMinutes) {
        this.timeout = idleMinutes;
    }

    public int getIdleMinutes() {
        return timeout;
    }

    @Override
    public long check(@Nonnull DockerSwarmComputer c) {
        if (c.isIdle() && c.isOnline() && isTaskCompleted) {
            final long idleMilliseconds = System.currentTimeMillis() - c.getIdleStartMilliseconds();
            if (idleMilliseconds > MINUTES.toMillis(timeout)) {
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
    }

    @Override
    public void taskCompleted(Executor executor, Queue.Task task, long durationMS) {
        this.isTaskCompleted = true;
        getLogger(executor).println("Task completed: " + task.getFullDisplayName());
        done(executor);
    }

    private PrintStream getLogger(Executor executor) {
        final DockerSwarmComputer c = (DockerSwarmComputer) executor.getOwner();
        return c.getListener().getLogger();
    }

    @Override
    public void taskCompletedWithProblems(Executor executor, Queue.Task task, long durationMS, Throwable problems) {
        taskCompleted(executor,task,durationMS);
    }

    private void done(Executor executor) {
        final DockerSwarmComputer c = (DockerSwarmComputer) executor.getOwner();
        Queue.Executable exec = executor.getCurrentExecutable();
        if (exec instanceof ContinuableExecutable && ((ContinuableExecutable) exec).willContinue()) {
            LOGGER.log(Level.FINE, "not terminating {0} because {1} says it will be continued", new Object[]{c.getName(), exec});
            return;
        }
        LOGGER.log(Level.FINE, "terminating {0} since {1} seems to be finished", new Object[]{c.getName(), exec});
        done(c);
    }

    private synchronized void done(final DockerSwarmComputer c) {
        c.setAcceptingTasks(false); // just in case
        if (terminating) {
            return;
        }
        terminating = true;
        Computer.threadPoolForRemoting.submit(() -> {
            Queue.withLock( () -> {
                 DockerSwarmAgent node = c.getNode();
                if (node != null) {
                    try {
                        node.terminate();
                    }
                    catch (IOException e) {}
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
