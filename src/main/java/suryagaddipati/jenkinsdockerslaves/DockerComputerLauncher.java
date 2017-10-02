package suryagaddipati.jenkinsdockerslaves;


import akka.actor.ActorRef;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import suryagaddipati.jenkinsdockerslaves.docker.CreateContainerRequest;
import suryagaddipati.jenkinsdockerslaves.docker.DockerAgentLauncher;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

public class DockerComputerLauncher extends ComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(DockerComputerLauncher.class.getName());
    private final String label;


    private final String jobName;


    private final Queue.BuildableItem bi;


    public DockerComputerLauncher(final Queue.BuildableItem bi) {
        this.bi = bi;
        this.label = bi.task.getAssignedLabel().getName();
        this.jobName = bi.task instanceof AbstractProject ? ((AbstractProject) bi.task).getFullName() : bi.task.getName();
    }

    @Override
    public void launch(final SlaveComputer computer, final TaskListener listener) throws IOException, InterruptedException {
        if (computer instanceof DockerComputer) {
            launch((DockerComputer) computer, listener);
        } else {
            throw new IllegalArgumentException("This launcher only can handle DockerComputer");
        }
    }

    private void launch(final DockerComputer computer, final TaskListener listener) throws IOException, InterruptedException {
        DockerSlaveInfo dockerSlaveInfo = null;
            dockerSlaveInfo = this.bi.getAction(DockerSlaveInfo.class);
            dockerSlaveInfo.setComputerLaunchTime(new Date());
            final DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
            if (this.bi.task instanceof AbstractProject) {
                ((AbstractProject) this.bi.task).setCustomWorkspace(configuration.getBaseWorkspaceLocation());
            }

            final LabelConfiguration labelConfiguration = configuration.getLabelConfiguration(this.label);

            final String[] envVarOptions = labelConfiguration.getEnvVarsConfig();
            final String[] envVars = new String[envVarOptions.length];
            if (envVarOptions.length != 0) {
                System.arraycopy(envVarOptions, 0, envVars, 0, envVarOptions.length);
            }

            final String additionalSlaveOptions = "-noReconnect";
            final String slaveOptions = "-jnlpUrl " + getSlaveJnlpUrl(computer, configuration) + " -secret " + getSlaveSecret(computer) + " " + additionalSlaveOptions;
            final String[] command = new String[]{"sh", "-c", "curl --connect-timeout 20  --max-time 60 -o slave.jar " + getSlaveJarUrl(configuration) + " && java -jar slave.jar " + slaveOptions};
            launchContainer(command,computer.getName(), envVars, labelConfiguration, listener);

//            lauchDocker(computer, listener, dockerSlaveInfo, configuration, labelConfiguration, envVars, command);

    }
    public void launchContainer(String[] command, String name, String[] envVars, LabelConfiguration labelConfiguration, TaskListener listener) {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        CreateContainerRequest crReq = new CreateContainerRequest(labelConfiguration.getImage(), command, envVars);

        final ActorRef agentLauncher = swarmPlugin.getActorSystem().actorOf(DockerAgentLauncher.props(listener.getLogger()), name);
        agentLauncher.tell(crReq,ActorRef.noSender());
    }

    private void launchAndConnect(DockerComputer computer, TaskListener listener, DockerSlaveInfo dockerSlaveInfo, DockerSlaveConfiguration configuration, LabelConfiguration labelConfiguration, String[] envVars, String[] command, DockerClient dockerClient) {
        final CreateContainerCmd containerCmd = dockerClient
                .createContainerCmd(labelConfiguration.getImage())
                .withCmd(command)
                .withPrivileged(configuration.isPrivileged())
                .withName(computer.getName())
                .withEnv(envVars);

        final String[] bindOptions = labelConfiguration.getHostBindsConfig();
        final String[] cacheDirs = labelConfiguration.getCacheDirs();
        final Bind[] binds = new Bind[bindOptions.length + cacheDirs.length];
        if (bindOptions.length != 0) {
            for (int i = 0; i < bindOptions.length; i++) {
                final String[] bindConfig = bindOptions[i].split(":");
                binds[i] = new Bind(bindConfig[0], new Volume(bindConfig[1]));
            }
        }

        createCacheBindings(listener, containerCmd, computer, cacheDirs, binds);
        containerCmd.withBinds(binds);


        setCgroupLimits(labelConfiguration, containerCmd, dockerSlaveInfo);

        if (StringUtils.isNotEmpty(labelConfiguration.getNetwork())) {
            containerCmd.withNetworkMode(labelConfiguration.getNetwork());
        }
        listener.getLogger().println("Creating Container :" + containerCmd.toString());
        final CreateContainerResponse container = containerCmd.exec();
        listener.getLogger().println("Created container :" + container.getId());
        computer.setContainerId(container.getId());

        setNetwork(configuration, dockerClient, container);

        final WaitContainerResultCallback createResponse = new WaitContainerResultCallback();
        dockerClient.waitContainerCmd(container.getId()).exec(createResponse);
        final Integer createStatusCode = createResponse.awaitStatusCode();
        if (createStatusCode != 0) {
            throw new RuntimeException("Container creation failed with error code: " + createStatusCode);
        }


        final InspectContainerResponse[] containerInfo = {null};
        ExceptionHandlingHelpers.executeWithRetryOnError(() -> containerInfo[0] = dockerClient.inspectContainerCmd(container.getId()).exec());
        computer.setNodeName(containerInfo[0].toString());
        dockerSlaveInfo.setContainerInfo(containerInfo[0]);

        dockerClient.startContainerCmd(container.getId()).exec();
        dockerSlaveInfo.setProvisionedTime(new Date());
        dockerSlaveInfo.setDockerImage(labelConfiguration.getImage());

//                computer.connect(false).get();
        computer.connect(false);
    }

    private void setNetwork(DockerSlaveConfiguration configuration, DockerClient dockerClient, CreateContainerResponse container) {
        if(StringUtils.isNotEmpty(configuration.getSwarmNetwork())){
            dockerClient.connectToNetworkCmd().withContainerId(container.getId()).withNetworkId(configuration.getSwarmNetwork()).exec();
        }
    }

    private void setCgroupLimits(final LabelConfiguration labelConfiguration, final CreateContainerCmd containerCmd, final DockerSlaveInfo dockerSlaveInfo) {
        Integer cpuAllocation = labelConfiguration.getMaxCpuShares();
        Long memoryAllocation = labelConfiguration.getMaxMemory();

        if (labelConfiguration.isDynamicResourceAllocation()) {
            final Run lastSuccessfulBuild = null; //job.getLastSuccessfulBuild();
            if (lastSuccessfulBuild != null && lastSuccessfulBuild.getAction(DockerSlaveInfo.class) != null) {
                final DockerSlaveInfo lastSuccessfulSlaveInfo = lastSuccessfulBuild.getAction(DockerSlaveInfo.class);
                cpuAllocation = Math.min(labelConfiguration.getMaxCpuShares(), lastSuccessfulSlaveInfo.getNextCpuAllocation());
                memoryAllocation = Math.min(labelConfiguration.getMaxMemory(), lastSuccessfulSlaveInfo.getNextMemoryAllocation());
            }
        }
        containerCmd.withCpuShares(cpuAllocation);
        containerCmd.withMemory(memoryAllocation);
        dockerSlaveInfo.setAllocatedCPUShares(cpuAllocation);
        dockerSlaveInfo.setAllocatedMemory(memoryAllocation);

    }

    private void createCacheBindings(final TaskListener listener, final CreateContainerCmd createContainerCmd, final DockerComputer computer, final String[] cacheDirs, final Bind[] binds) {
        if (cacheDirs.length > 0) {
            final String cacheVolumeName = getJobName() + "-" + computer.getName();
            createContainerCmd.withVolumeDriver("cache-driver");
            this.bi.getAction(DockerSlaveInfo.class).setCacheVolumeName(cacheVolumeName);
            for (int i = 0; i < cacheDirs.length; i++) {
                listener.getLogger().println("Binding Volume" + cacheDirs[i] + " to " + cacheVolumeName);
                binds[binds.length - 1] = new Bind(cacheVolumeName, new Volume(cacheDirs[i]));
            }
        }
    }

    private boolean noResourcesAvailable(final Throwable e) {
        return e instanceof InternalServerErrorException && e.getMessage().trim().contains("no resources available to schedule container");
    }


    private String getSlaveJarUrl(final DockerSlaveConfiguration configuration) {
        return getJenkinsUrl(configuration) + "jnlpJars/slave.jar";
    }

    private String getSlaveJnlpUrl(final Computer computer, final DockerSlaveConfiguration configuration) {
        return getJenkinsUrl(configuration) + computer.getUrl() + "slave-agent.jnlp";

    }

    private String getSlaveSecret(final Computer computer) {
        return ((DockerComputer) computer).getJnlpMac();

    }

    private String getJenkinsUrl(final DockerSlaveConfiguration configuration) {
        final String url = configuration.getJenkinsUrl();
        if (url.endsWith("/")) {
            return url;
        } else {
            return url + '/';
        }
    }

    public String getJobName() {
        return this.jobName
                .replaceAll("/", "_")
                .replaceAll("-", "_")
                .replaceAll(",", "_")
                .replaceAll(" ", "_")
                .replaceAll("=", "_")
                .replaceAll("\\.", "_");
    }
}
