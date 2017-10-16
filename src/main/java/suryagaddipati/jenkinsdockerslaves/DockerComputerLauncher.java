package suryagaddipati.jenkinsdockerslaves;


import akka.actor.ActorRef;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.CreateServiceRequest;

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
            final String[] command = new String[]{"sh", "-cx", "curl --connect-timeout 20  --max-time 60 -o slave.jar " + getSlaveJarUrl(configuration) + " && java -jar slave.jar " + slaveOptions};
            launchContainer(command,configuration, envVars, labelConfiguration, listener, computer);
    }

    public void launchContainer(String[] command, DockerSlaveConfiguration configuration, String[] envVars, LabelConfiguration labelConfiguration, TaskListener listener, DockerComputer computer) {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        CreateServiceRequest crReq = new CreateServiceRequest(computer.getName(), labelConfiguration.getImage(), command, envVars);

        crReq.setTaskLimits(labelConfiguration.getLimitsNanoCPUs(),labelConfiguration.getLimitsMemoryBytes() );
        crReq.setTaskReservations(labelConfiguration.getReservationsNanoCPUs(),labelConfiguration.getReservationsMemoryBytes() );

        String[] hostBinds = labelConfiguration.getHostBindsConfig();
        for(int i = 0; i < hostBinds.length; i++){
           String hostBind = hostBinds[i];
            String[] srcDest = hostBind.split(":");
            crReq.addBindVolume(srcDest[0],srcDest[1]);
        }
//        if(StringUtils.isNotEmpty(configuration.getSwarmNetwork())){
//            crReq.HostConfig.NetworkMode = configuration.getSwarmNetwork();
//        }

        final String[] cacheDirs = labelConfiguration.getCacheDirs();
        if (cacheDirs.length > 0) {
            final String cacheVolumeName = getJobName() + "-" + computer.getName();
            this.bi.getAction(DockerSlaveInfo.class).setCacheVolumeName(cacheVolumeName);
            for (int i = 0; i < cacheDirs.length; i++) {
                listener.getLogger().println("Binding Volume" + cacheDirs[i] + " to " + cacheVolumeName);
//                crReq.HostConfig.addCacheMount(cacheVolumeName, cacheDirs[i]);
            }
        }

        final ActorRef agentLauncher = swarmPlugin.getActorSystem().actorOf(DockerAgentLauncherActor.props(listener.getLogger()), computer.getName());
        agentLauncher.tell(crReq,ActorRef.noSender());
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
