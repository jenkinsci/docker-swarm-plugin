package suryagaddipati.jenkinsdockerslaves;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Statistics;
import hudson.Extension;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import jenkins.model.Jenkins;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ContainerCleanupListener extends RunListener<Run<?,?>> {
    private static final Logger LOGGER = Logger.getLogger(DockerComputer .class.getName());

    @Override
    public void onCompleted(Run<?, ?> run, @Nonnull TaskListener listener) {
        if(run.getAction(DockerLabelAssignmentAction.class) !=null){
            DockerLabelAssignmentAction labelAssignmentAction = run.getAction(DockerLabelAssignmentAction.class);
            final String computerName = labelAssignmentAction.getLabel().getName();
            final PrintStream logger = listener.getLogger();
            DockerComputer computer = (DockerComputer) Jenkins.getInstance().getComputer(computerName);
            try {
                terminate(computer,logger);
            } catch (IOException|InterruptedException  e) {
                e.printStackTrace(listener.getLogger());

                LOGGER.log(Level.INFO,"Failed to Cleanup Run "+ run.getFullDisplayName(),e);
            }
        }
    }

    public void terminate(DockerComputer computer, PrintStream logger) throws IOException, InterruptedException {
        computer.setAcceptingTasks(false);
        gatherStats(computer,logger);
        cleanupNode(computer,logger);
        cleanupDockerVolumeAndContainer(computer,logger);
    }

    private void cleanupNode(DockerComputer computer, PrintStream logger) throws IOException, InterruptedException {
            if(computer.getNode() !=null){
                logger.println("Removing node "+ computer.getNode().getDisplayName());
                computer.getNode().terminate();
            }
    }

    private void cleanupDockerVolumeAndContainer(DockerComputer computer, PrintStream logger) throws IOException {
        computer.cleanupDockerContainer(logger);
    }

    private void gatherStats(DockerComputer computer, PrintStream logger) throws IOException {
        DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
        try( DockerClient dockerClient = configuration.newDockerClient()){
            String containerId = computer.getContainerId();
            Queue.Executable currentExecutable = computer.getExecutors().get(0).getCurrentExecutable();
            if(currentExecutable instanceof Run && ((Run)currentExecutable).getAction(DockerSlaveInfo.class) != null){
                Run run = ((Run) currentExecutable);
                DockerSlaveInfo slaveInfo = ((Run) currentExecutable).getAction(DockerSlaveInfo.class);
                Statistics stats = dockerClient.statsCmd(containerId).exec();
                slaveInfo.setStats(stats);
                run.save();
            }
        }
    }



}
