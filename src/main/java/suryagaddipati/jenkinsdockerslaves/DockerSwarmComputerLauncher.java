package suryagaddipati.jenkinsdockerslaves;


import akka.actor.ActorRef;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.Service;

import java.io.IOException;
import java.util.Date;

public class DockerSwarmComputerLauncher extends JNLPLauncher {

    private final String label;
    private final String jobName;
    private final Queue.BuildableItem bi;


    public DockerSwarmComputerLauncher(final Queue.BuildableItem bi) {
        this.bi = bi;
        this.label = bi.task.getAssignedLabel().getName();
        this.jobName = bi.task instanceof AbstractProject ? ((AbstractProject) bi.task).getFullName() : bi.task.getName();
    }

    @Override
    public void launch(final SlaveComputer computer, final TaskListener listener){
        if (computer instanceof DockerComputer) {
            launch((DockerComputer) computer, listener);
        } else {
            throw new IllegalArgumentException("This launcher only can handle DockerComputer");
        }
    }

    private void launch(final DockerComputer computer, final TaskListener listener) {
        DockerSlaveInfo dockerSlaveInfo = null;
        dockerSlaveInfo = this.bi.getAction(DockerSlaveInfo.class);
        dockerSlaveInfo.setComputerLaunchTime(new Date());
        final DockerSwarmCloud configuration = DockerSwarmCloud.get();
        final LabelConfiguration labelConfiguration = configuration.getLabelConfiguration(this.label);

        setBaseWorkspaceLocation(labelConfiguration);

        final String[] envVarOptions = labelConfiguration.getEnvVarsConfig();
        final String[] envVars = new String[envVarOptions.length];
        if (envVarOptions.length != 0) {
            System.arraycopy(envVarOptions, 0, envVars, 0, envVarOptions.length);
        }

        final String additionalSlaveOptions = "-noReconnect -workDir /tmp ";
        final String slaveOptions = "-jnlpUrl " + getSlaveJnlpUrl(computer, configuration) + " -secret " + getSlaveSecret(computer) + " " + additionalSlaveOptions;
        final String[] command = new String[]{"sh", "-cx", "curl --connect-timeout 20  --max-time 60 -o slave.jar " + getSlaveJarUrl(configuration) + " && java -jar slave.jar " + slaveOptions};
        launchContainer(command,configuration, envVars, labelConfiguration, listener, computer);
    }

    private void setBaseWorkspaceLocation(LabelConfiguration labelConfiguration){
        if (this.bi.task instanceof AbstractProject && StringUtils.isNotEmpty(labelConfiguration.getBaseWorkspaceLocation())) {
            try {
                ((AbstractProject) this.bi.task).setCustomWorkspace(labelConfiguration.getBaseWorkspaceLocation());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void launchContainer(String[] commands, DockerSwarmCloud configuration, String[] envVars, LabelConfiguration labelConfiguration, TaskListener listener, DockerComputer computer) {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        Service crReq = createCreateServiceRequest(commands, configuration, envVars, labelConfiguration, computer);

        setLimitsAndReservations(labelConfiguration, crReq);
        setHostBinds(labelConfiguration, crReq);
        setNetwork(configuration, crReq);
        setCacheDirs(configuration, labelConfiguration, listener, computer, crReq);
        setTmpfs(labelConfiguration, crReq);
        setConstraints(labelConfiguration,crReq);
        setLabels(crReq);

        final ActorRef agentLauncher = swarmPlugin.getActorSystem().actorOf(DockerAgentLauncherActor.props(listener.getLogger()), computer.getName());
        agentLauncher.tell(crReq,ActorRef.noSender());
    }

    private void setLabels(Service crReq) {
        crReq.addLabel("ROLE","jenkins-agent");
    }

    private void setConstraints(LabelConfiguration labelConfiguration, Service crReq) {
        crReq.TaskTemplate.setPlacementConstraints(labelConfiguration.getPlacementConstraintsConfig());
    }

    private Service createCreateServiceRequest(String[] commands, DockerSwarmCloud configuration, String[] envVars, LabelConfiguration labelConfiguration, DockerComputer computer) {
        Service crReq;
        if(labelConfiguration.getLabel().contains("dind")){
            commands[2]= StringUtils.isEmpty(configuration.getSwarmNetwork())?
                    String.format("docker run --rm --privileged %s sh -xc '%s' ",labelConfiguration.getImage(), commands[2]):
                    String.format("docker run --rm --privileged --network %s %s sh -xc '%s' ",configuration.getSwarmNetwork(), labelConfiguration.getImage(), commands[2]);

            crReq = new Service(computer.getName(),"docker:17.12" , commands, envVars);
        }else {
            crReq = new Service(computer.getName(), labelConfiguration.getImage(), commands, envVars);
        }
        return crReq;
    }

    private void setTmpfs(LabelConfiguration labelConfiguration, Service crReq) {
        if(StringUtils.isNotEmpty(labelConfiguration.getTmpfsDir())){
            crReq.addTmpfsMount(labelConfiguration.getTmpfsDir());
        }
    }

    private void setCacheDirs(DockerSwarmCloud configuration, LabelConfiguration labelConfiguration, TaskListener listener, DockerComputer computer, Service crReq) {
        final String[] cacheDirs = labelConfiguration.getCacheDirs();
        if (cacheDirs.length > 0) {
            final String cacheVolumeName = getJobName() + "-" + computer.getVolumeName();
            this.bi.getAction(DockerSlaveInfo.class).setCacheVolumeName(cacheVolumeName);
            for (int i = 0; i < cacheDirs.length; i++) {
                listener.getLogger().println("Binding Volume" + cacheDirs[i] + " to " + cacheVolumeName);
                crReq.addCacheVolume(cacheVolumeName, cacheDirs[i], configuration.getCacheDriverName());
            }
        }
    }

    private void setNetwork(DockerSwarmCloud configuration, Service crReq) {
        crReq.setNetwork(configuration.getSwarmNetwork());
    }

    private void setHostBinds(LabelConfiguration labelConfiguration, Service crReq) {
        String[] hostBinds = labelConfiguration.getHostBindsConfig();
        for(int i = 0; i < hostBinds.length; i++){
            String hostBind = hostBinds[i];
            String[] srcDest = hostBind.split(":");
            crReq.addBindVolume(srcDest[0],srcDest[1]);
        }
    }

    private void setLimitsAndReservations(LabelConfiguration labelConfiguration, Service crReq) {
        crReq.setTaskLimits(labelConfiguration.getLimitsNanoCPUs(),labelConfiguration.getLimitsMemoryBytes() );
        crReq.setTaskReservations(labelConfiguration.getReservationsNanoCPUs(),labelConfiguration.getReservationsMemoryBytes() );
    }


    private String getSlaveJarUrl(final DockerSwarmCloud configuration) {
        return getJenkinsUrl(configuration) + "jnlpJars/slave.jar";
    }

    private String getSlaveJnlpUrl(final Computer computer, final DockerSwarmCloud configuration) {
        return getJenkinsUrl(configuration) + computer.getUrl() + "slave-agent.jnlp";

    }

    private String getSlaveSecret(final Computer computer) {
        return ((DockerComputer) computer).getJnlpMac();

    }

    private String getJenkinsUrl(final DockerSwarmCloud configuration) {
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

    public Queue.BuildableItem getBi() {
        return bi;
    }
}
