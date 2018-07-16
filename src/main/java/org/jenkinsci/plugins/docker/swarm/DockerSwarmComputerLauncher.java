package org.jenkinsci.plugins.docker.swarm;


import akka.actor.ActorRef;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ServiceSpec;

import java.io.IOException;

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
        if (computer instanceof DockerSwarmComputer) {
            launch((DockerSwarmComputer) computer, listener);
        } else {
            throw new IllegalArgumentException("This launcher only can handle DockerSwarmComputer");
        }
    }

    private void launch(final DockerSwarmComputer computer, final TaskListener listener) {
        final DockerSwarmCloud configuration = DockerSwarmCloud.get();
        final DockerSwarmAgentTemplate dockerSwarmAgentTemplate = configuration.getLabelConfiguration(this.label);

        setBaseWorkspaceLocation(dockerSwarmAgentTemplate);

        final String[] envVarOptions = dockerSwarmAgentTemplate.getEnvVarsConfig();
        final String[] envVars = new String[envVarOptions.length];
        if (envVarOptions.length != 0) {
            System.arraycopy(envVarOptions, 0, envVars, 0, envVarOptions.length);
        }

        final String additionalAgentOptions = "-noReconnect -workDir /tmp ";
        final String agentOptions = "-jnlpUrl " + getAgentJnlpUrl(computer, configuration) + " -secret " + getAgentSecret(computer) + " " + additionalAgentOptions;
        final String[] command = new String[]{"sh", "-cx", "curl --connect-timeout 20  --max-time 60 -o slave.jar " + getAgentJarUrl(configuration) + " && java -jar slave.jar " + agentOptions};
        launchContainer(command,configuration, envVars, dockerSwarmAgentTemplate, listener, computer);
    }

    private void setBaseWorkspaceLocation(DockerSwarmAgentTemplate dockerSwarmAgentTemplate){
        if (this.bi.task instanceof AbstractProject && StringUtils.isNotEmpty(dockerSwarmAgentTemplate.getBaseWorkspaceLocation())) {
            try {
                ((AbstractProject) this.bi.task).setCustomWorkspace(dockerSwarmAgentTemplate.getBaseWorkspaceLocation());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void launchContainer(String[] commands, DockerSwarmCloud configuration, String[] envVars, DockerSwarmAgentTemplate dockerSwarmAgentTemplate, TaskListener listener, DockerSwarmComputer computer) {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        ServiceSpec crReq = createCreateServiceRequest(commands, configuration, envVars, dockerSwarmAgentTemplate, computer);

        setLimitsAndReservations(dockerSwarmAgentTemplate, crReq);
        setHostBinds(dockerSwarmAgentTemplate, crReq);
        setNetwork(configuration, crReq);
        setCacheDirs(configuration, dockerSwarmAgentTemplate, listener, computer, crReq);
        setTmpfs(dockerSwarmAgentTemplate, crReq);
        setConstraints(dockerSwarmAgentTemplate,crReq);
        setLabels(crReq);
        setRestartAttemptCount(crReq);
        setAuthHeaders(dockerSwarmAgentTemplate, crReq);

        final ActorRef agentLauncher = swarmPlugin.getActorSystem().actorOf(DockerSwarmAgentLauncherActor.props(listener.getLogger()), computer.getName());
        agentLauncher.tell(crReq,ActorRef.noSender());
    }

    private void setRestartAttemptCount(ServiceSpec crReq) {
        crReq.TaskTemplate.setRestartAttemptCount(500);
    }

    private void setLabels(ServiceSpec crReq) {
        crReq.addLabel("ROLE","jenkins-agent");
    }

    private void setConstraints(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        crReq.TaskTemplate.setPlacementConstraints(dockerSwarmAgentTemplate.getPlacementConstraintsConfig());
    }

    private ServiceSpec createCreateServiceRequest(String[] commands, DockerSwarmCloud configuration, String[] envVars, DockerSwarmAgentTemplate dockerSwarmAgentTemplate, DockerSwarmComputer computer) {
        ServiceSpec crReq;
        if(dockerSwarmAgentTemplate.getLabel().contains("dind")){
            commands[2]= StringUtils.isEmpty(configuration.getSwarmNetwork())?
                    String.format("docker run --rm --privileged %s sh -xc '%s' ", dockerSwarmAgentTemplate.getImage(), commands[2]):
                    String.format("docker run --rm --privileged --network %s %s sh -xc '%s' ",configuration.getSwarmNetwork(), dockerSwarmAgentTemplate.getImage(), commands[2]);

            crReq = new ServiceSpec(computer.getName(),"docker:17.12" , commands, envVars);
        }else {
            crReq = new ServiceSpec(computer.getName(), dockerSwarmAgentTemplate.getImage(), commands, envVars);
        }
        return crReq;
    }

    private void setTmpfs(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        if(StringUtils.isNotEmpty(dockerSwarmAgentTemplate.getTmpfsDir())){
            crReq.addTmpfsMount(dockerSwarmAgentTemplate.getTmpfsDir());
        }
    }

    private void setCacheDirs(DockerSwarmCloud configuration, DockerSwarmAgentTemplate dockerSwarmAgentTemplate, TaskListener listener, DockerSwarmComputer computer, ServiceSpec crReq) {
        final String[] cacheDirs = dockerSwarmAgentTemplate.getCacheDirs();
        if (cacheDirs.length > 0) {
            final String cacheVolumeName = getJobName() + "-" + computer.getVolumeName();
            this.bi.getAction(DockerSwarmAgentInfo.class).setCacheVolumeName(cacheVolumeName);
            for (int i = 0; i < cacheDirs.length; i++) {
                listener.getLogger().println("Binding Volume" + cacheDirs[i] + " to " + cacheVolumeName);
                crReq.addCacheVolume(cacheVolumeName, cacheDirs[i], configuration.getCacheDriverName());
            }
        }
    }

    private void setNetwork(DockerSwarmCloud configuration, ServiceSpec crReq) {
        crReq.setNetwork(configuration.getSwarmNetwork());
    }

    private void setHostBinds(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        String[] hostBinds = dockerSwarmAgentTemplate.getHostBindsConfig();
        for(int i = 0; i < hostBinds.length; i++){
            String hostBind = hostBinds[i];
            String[] srcDest = hostBind.split(":");
            crReq.addBindVolume(srcDest[0],srcDest[1]);
        }
    }

    private void setAuthHeaders(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        crReq.setAuthHeader(
                dockerSwarmAgentTemplate.getUsername(),
                dockerSwarmAgentTemplate.getPassword(),
                dockerSwarmAgentTemplate.getEmail(),
                dockerSwarmAgentTemplate.getServerAddress()
        );
    }

    private void setLimitsAndReservations(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        crReq.setTaskLimits(dockerSwarmAgentTemplate.getLimitsNanoCPUs(), dockerSwarmAgentTemplate.getLimitsMemoryBytes() );
        crReq.setTaskReservations(dockerSwarmAgentTemplate.getReservationsNanoCPUs(), dockerSwarmAgentTemplate.getReservationsMemoryBytes() );
    }


    private String getAgentJarUrl(final DockerSwarmCloud configuration) {
        return getJenkinsUrl(configuration) + "jnlpJars/slave.jar";
    }

    private String getAgentJnlpUrl(final Computer computer, final DockerSwarmCloud configuration) {
        return getJenkinsUrl(configuration) + computer.getUrl() + "slave-agent.jnlp";

    }

    private String getAgentSecret(final Computer computer) {
        return ((DockerSwarmComputer) computer).getJnlpMac();

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

}
