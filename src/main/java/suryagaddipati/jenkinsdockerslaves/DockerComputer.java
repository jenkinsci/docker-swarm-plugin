
package suryagaddipati.jenkinsdockerslaves;

import com.google.common.collect.Iterables;
import hudson.model.Executor;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.OfflineCause;
import hudson.util.StreamTaskListener;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class DockerComputer extends AbstractCloudComputer<DockerSlave> {

    public DockerComputer(final DockerSlave dockerSlave) {
        super(dockerSlave);
    }


    public Queue.Executable getCurrentBuild() {
        if (!Iterables.isEmpty(getExecutors())) {
            final Executor exec = getExecutors().get(0);
            return exec.getCurrentExecutable() == null ? null : exec.getCurrentExecutable();
        }
        return null;
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
    public void setChannel(final Channel channel, final OutputStream launchLog, final Channel.Listener listener) throws IOException, InterruptedException {
        final TaskListener taskListener = new StreamTaskListener(launchLog);
        channel.addListener(new Channel.Listener() {
            @Override
            public void onClosed(final Channel channel, final IOException cause) {
               DockerComputerLauncher launcher = (DockerComputerLauncher) getLauncher();
                Queue.BuildableItem queueItem = launcher.getBi();
                if(cause != null){
                //    Jenkins.getInstance().getQueue().schedule2(queueItem.task,0, (List<Action>) queueItem.getAllActions());
                }
            }
        });
        super.setChannel(channel, launchLog, listener);
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
    public Future<?> disconnect(OfflineCause cause) {
        return CompletableFuture.completedFuture(true);
    }



    public String getVolumeName() {
        return getName().split("-")[1];
    }
}
