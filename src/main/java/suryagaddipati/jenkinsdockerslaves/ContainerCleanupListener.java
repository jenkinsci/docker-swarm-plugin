package suryagaddipati.jenkinsdockerslaves;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
        cleanupDockerContainer(computer,logger);
    }

    private void cleanupNode(DockerComputer computer, PrintStream logger) throws IOException, InterruptedException {
            if(computer.getNode() !=null){
                logger.println("Removing node "+ computer.getNode().getDisplayName());
                computer.getNode().terminate();
            }
    }

    private void cleanupDockerContainer(DockerComputer computer, PrintStream logger) {
        final ExecutorService threadPool = Executors.newSingleThreadExecutor();
        threadPool.submit((Runnable) () -> {
            try {
                cleanupDockerContainer(computer.getContainerId(), logger);
            } catch (IOException e) {
                LOGGER.log(Level.INFO,"couldn't cleanup container ",e);
            }
        });
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
    public void cleanupDockerContainer(String  containerId, PrintStream logger) throws IOException {

        DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
        try( DockerClient dockerClient = configuration.newDockerClient()){
            if (containerId != null){
                InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
                if( container.getState().getPaused()){
                    DockerApiHelpers.executeSliently(() ->  dockerClient.unpauseContainerCmd(containerId).exec());
                }
                DockerApiHelpers.executeSliently(() -> dockerClient.killContainerCmd(containerId).exec());
                DockerApiHelpers.executeSlientlyWithLogging(()->removeContainer(logger, containerId, dockerClient), LOGGER,"Failed to cleanup container : " + containerId);
            }
        }
    }
    private void removeContainer(PrintStream logger, String containerId, DockerClient dockerClient) {
        DockerApiHelpers.executeWithRetryOnError(() -> dockerClient.removeContainerCmd(containerId).exec() );
        logger.println("Removed Container " + containerId);
    }


}
