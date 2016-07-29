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

    private static final Logger LOGGER = Logger.getLogger(ContainerCleanupListener.class.getName());
    @Override
    public void onCompleted(Run<?, ?> run, @Nonnull TaskListener listener) {
        if(run.getAction(DockerLabelAssignmentAction.class) !=null){
            DockerLabelAssignmentAction labelAssignmentAction = run.getAction(DockerLabelAssignmentAction.class);
            final String computerName = labelAssignmentAction.getLabel().getName();
            final PrintStream logger = listener.getLogger();
            DockerComputer computer = (DockerComputer) Jenkins.getInstance().getComputer(computerName);
            terminate(computer,logger);
        }
    }

    public void terminate(DockerComputer computer, PrintStream logger) {
        computer.setAcceptingTasks(false);
        gatherStats(computer,logger);
        cleanupNode(computer,logger);
        cleanupDockerVolumeAndContainer(computer,logger);
    }

    private void cleanupNode(DockerComputer computer, PrintStream logger) {
        try {
            if(computer.getNode() !=null){
                logger.println("Removing node "+ computer.getNode().getDisplayName());
                computer.getNode().terminate();
            }
        } catch (InterruptedException | IOException e) {
            LOGGER.log(Level.INFO,"Failed to remove node : " +computer.getNode(),e);
            e.printStackTrace(logger);
        }
    }

    private void cleanupDockerVolumeAndContainer(DockerComputer computer, PrintStream logger) {
        String containerId = computer.getContainerId();
        String volumeName = computer.getVolumeName();
        DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
        try( DockerClient dockerClient = configuration.newDockerClient()){
            if (containerId != null){
                killContainer(containerId, dockerClient);
                removeContainer(logger, containerId, dockerClient);
            }
            removeVolume(logger, volumeName, dockerClient);
        } catch (Exception e) {
            LOGGER.log(Level.INFO,"Failed to cleanup container : " +computer.getName(),e);
            e.printStackTrace(logger);
        }
    }

    private void gatherStats(DockerComputer computer, PrintStream logger) {
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
        }catch (Exception e){
            LOGGER.log(Level.INFO,"Failed to gather stats : " +computer.getName(),e);
            e.printStackTrace(logger);
        }
    }

    private void removeVolume(PrintStream logger, String volumeName, DockerClient dockerClient) {
        if(volumeName != null){
            try{
                dockerClient.removeVolumeCmd(volumeName).exec();
                logger.println("Removed volume " + volumeName);
            }catch (Exception e){ // seems like container rm is not reliable, sometimes we see 'container still in use' despite api saying otherwise
                try {
                    Thread.sleep(5000);
                    dockerClient.removeVolumeCmd(volumeName).exec();
                    logger.println("Removed volume " + volumeName);
                } catch (InterruptedException e1 ) {
                }
            }
        }
    }

    private void removeContainer(PrintStream logger, String containerId, DockerClient dockerClient) {
        dockerClient.removeContainerCmd(containerId).exec();
        logger.println("Removed Container " + containerId);
    }

    private void killContainer(String containerId, DockerClient dockerClient) {
        try {
            dockerClient.killContainerCmd(containerId).exec();
        }catch (Exception _){}
    }
}
