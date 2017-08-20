package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import javax.annotation.Nonnull;
import java.io.IOException;

@Extension
public class RunListenerToDeleteComputer extends RunListener<Run<?, ?>> {

    @Override
    public void onCompleted(final Run<?, ?> run, @Nonnull final TaskListener listener) {

        if (run.getAction(DockerSlaveInfo.class) != null) {
            final DockerSlaveInfo slaveInfo = run.getAction(DockerSlaveInfo.class);

//            final Statistics stats = dockerClient.statsCmd(slaveInfo.getContainerId()).exec();
//            slaveInfo.setStats(stats);
            try {
                run.save();
            } catch (final IOException e) {

            }
        }
    }
}
