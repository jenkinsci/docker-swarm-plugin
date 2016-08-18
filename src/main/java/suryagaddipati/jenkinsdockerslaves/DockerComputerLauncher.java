package suryagaddipati.jenkinsdockerslaves;


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerComputerLauncher extends ComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(DockerComputerLauncher.class.getName());
    private String label;


    private String jobName;


    private AbstractProject job;
    private Queue.BuildableItem bi;


    public DockerComputerLauncher(Queue.BuildableItem bi) {
        this.bi = bi;
        this.job = ((AbstractProject)bi.task);
        this.label = job.getAssignedLabel().getName();
        this.jobName = job.getFullName();
    }

    @Override
    public void launch(final SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        if (computer instanceof DockerComputer) {
            launch((DockerComputer) computer, listener);
        } else {
            throw new IllegalArgumentException("This launcher only can handle DockerComputer");
        }
    }

    private void launch(final DockerComputer computer, TaskListener listener) throws IOException, InterruptedException {
        DockerSlaveInfo dockerSlaveInfo = null;
        try {
            setToInProgress(bi);
            dockerSlaveInfo = bi.getAction(DockerSlaveInfo.class);
            DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
            job.setCustomWorkspace(configuration.getBaseWorkspaceLocation());
            try(DockerClient dockerClient = configuration.newDockerClient()) {
                LabelConfiguration labelConfiguration = configuration.getLabelConfiguration(this.label);


                final String additionalSlaveOptions = "-noReconnect";
                final String slaveOptions = "-jnlpUrl " + getSlaveJnlpUrl(computer, configuration) + " -secret " + getSlaveSecret(computer) + " " + additionalSlaveOptions;
                final String[] command = new String[]{"sh", "-c", "curl -o slave.jar " + getSlaveJarUrl(configuration) + " && java -jar slave.jar " + slaveOptions};


                CreateContainerCmd containerCmd = dockerClient
                        .createContainerCmd(labelConfiguration.getImage())
                        .withCmd(command)
                        .withPrivileged(configuration.isPrivileged())
                        .withHostName(computer.getName())
                        .withName(computer.getName());

                String[] bindOptions = labelConfiguration.getHostBindsConfig();
                String[] cacheDirs = labelConfiguration.getCacheDirs();
                Bind[] binds = new Bind[bindOptions.length + cacheDirs.length];
                if (bindOptions.length != 0) {
                    for (int i = 0; i < bindOptions.length; i++) {
                        String[] bindConfig = bindOptions[i].split(":");
                        binds[i] = new Bind(bindConfig[0], new Volume(bindConfig[1]));
                    }
                }

                createCacheBindings(listener, containerCmd, computer, cacheDirs, binds);
                containerCmd.withBinds(binds);


                setCgroupLimits(labelConfiguration, containerCmd,dockerSlaveInfo);

                listener.getLogger().println("Creating Container :" + containerCmd.toString());
                CreateContainerResponse container = containerCmd.exec();
                listener.getLogger().println("Created container :" + container.getId());


                final InspectContainerResponse[] containerInfo = {null};
                DockerApiHelpers.executeWithRetryOnError(() -> containerInfo[0] = dockerClient.inspectContainerCmd(container.getId()).exec());
                computer.setNodeName(containerInfo[0].getNode().getName());
                dockerSlaveInfo.setContainerInfo(containerInfo[0]);

                dockerClient.startContainerCmd(container.getId()).exec();
                computer.setContainerId(container.getId());
                computer.connect(false).get(1, TimeUnit.MINUTES);
                dockerSlaveInfo.setProvisionedTime(new Date());
            }

        } catch (Throwable e) {
            new ContainerCleanupListener().terminate(computer, listener.getLogger());
            String build = bi + "-" + job.getNextBuildNumber();
            if(noResourcesAvailable(e)){
                LOGGER.info("Not resources available for :" + build);
            }else {
                LOGGER.log(Level.INFO,"Failed to schedule: " + build, e);
                dockerSlaveInfo.incrementProvisioningAttemptCount();
            }
            throw new RuntimeException(e);
        }finally {
            if(dockerSlaveInfo !=null)
                dockerSlaveInfo.setProvisioningInProgress(false);
        }
    }

    private void setCgroupLimits(LabelConfiguration labelConfiguration, CreateContainerCmd containerCmd, DockerSlaveInfo dockerSlaveInfo) {
        Integer cpuAllocation = labelConfiguration.getMaxCpuShares();
        Long memoryAllocation = labelConfiguration.getMaxMemory();

        if(labelConfiguration.isDynamicResourceAllocation()){
            Run lastSuccessfulBuild = job.getLastSuccessfulBuild();
            if(lastSuccessfulBuild !=null && lastSuccessfulBuild.getAction(DockerSlaveInfo.class)!=null){
                DockerSlaveInfo lastSuccessfulSlaveInfo = lastSuccessfulBuild.getAction(DockerSlaveInfo.class);
                cpuAllocation = Math.min(labelConfiguration.getMaxCpuShares(),  lastSuccessfulSlaveInfo.getNextCpuAllocation());
                memoryAllocation =  Math.min(labelConfiguration.getMaxMemory(), lastSuccessfulSlaveInfo.getNextMemoryAllocation());
            }
        }
        containerCmd.withCpuShares(cpuAllocation);
        containerCmd.withMemory(memoryAllocation);
        dockerSlaveInfo.setAllocatedCPUShares(cpuAllocation);
        dockerSlaveInfo.setAllocatedMemory(memoryAllocation);

    }

    private void createCacheBindings(TaskListener listener, CreateContainerCmd createContainerCmd, DockerComputer computer, String[] cacheDirs, Bind[] binds) {
        if(cacheDirs.length > 0){
            String cacheVolumeName = getJobName() + "-" + computer.getName();
            createContainerCmd.withVolumeDriver("cache-driver");
            computer.setVolumeName(cacheVolumeName);
            bi.getAction(DockerSlaveInfo.class).setCacheVolumeName(cacheVolumeName);
            for(int i = 0; i < cacheDirs.length ; i++){
                listener.getLogger().println("Binding Volume" + cacheDirs[i]+ " to " + cacheVolumeName);
                binds[binds.length-1] = new Bind(cacheVolumeName,new Volume(cacheDirs[i]));
            }
        }
    }

    private boolean noResourcesAvailable(Throwable e) {
        return e instanceof  InternalServerErrorException && e.getMessage().trim().contains("no resources available to schedule container");
    }

    private void setToInProgress(Queue.BuildableItem bi) {
        DockerSlaveInfo slaveInfoAction = bi.getAction(DockerSlaveInfo.class);
        if ( slaveInfoAction != null){
            slaveInfoAction.setProvisioningInProgress(true);
        }else{
            bi.replaceAction(new DockerSlaveInfo(true));
        }
    }


    private String getSlaveJarUrl(DockerSlaveConfiguration configuration) {
        return getJenkinsUrl(configuration) + "jnlpJars/slave.jar";
    }

    private String getSlaveJnlpUrl(Computer computer, DockerSlaveConfiguration configuration) {
        return getJenkinsUrl(configuration) + computer.getUrl() + "slave-agent.jnlp";

    }


    private String getSlaveSecret(Computer computer) {
        return ((DockerComputer)computer).getJnlpMac();

    }

    private String getJenkinsUrl(DockerSlaveConfiguration configuration) {
        String url = configuration.getJenkinsUrl();
        if (url.endsWith("/")) {
            return url;
        } else {
            return url + '/';
        }
    }

    public String getJobName() {
        return jobName
                .replaceAll("/","_")
                .replaceAll("-","_")
                .replaceAll(",","_")
                .replaceAll(" ","_")
                .replaceAll("=","_")
                .replaceAll("\\.","_");
    }
}
