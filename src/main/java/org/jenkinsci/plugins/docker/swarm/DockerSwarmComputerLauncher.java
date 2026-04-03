package org.jenkinsci.plugins.docker.swarm;

import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.swarm.docker.api.configs.Config;
import org.jenkinsci.plugins.docker.swarm.docker.api.configs.ListConfigsRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiException;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.SerializationException;
import org.jenkinsci.plugins.docker.swarm.docker.api.secrets.ListSecretsRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.secrets.Secret;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ServiceSpec;

import akka.actor.ActorRef;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.security.ACL;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import jenkins.slaves.RemotingWorkDirSettings;

public class DockerSwarmComputerLauncher extends JNLPLauncher {

    private final String label;
    private final String jobName;
    private final Queue.BuildableItem bi;
    private DockerSwarmAgentInfo agentInfo;

    private static final Logger LOGGER = Logger.getLogger(DockerSwarmComputerLauncher.class.getName());

    public DockerSwarmComputerLauncher(final Queue.BuildableItem bi) {
        super(DockerSwarmCloud.get().getTunnel(), null, new RemotingWorkDirSettings(false, "/tmp", null, false));
        this.bi = bi;
        this.label = bi.task.getAssignedLabel().getName();
        this.jobName = bi.task instanceof AbstractProject ? ((AbstractProject) bi.task).getFullName()
                : bi.task.getName();
    }

    @Override
    public void launch(final SlaveComputer computer, final TaskListener listener) {
        if (computer instanceof DockerSwarmComputer) {
            try {
                launch((DockerSwarmComputer) computer, listener);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to launch", e);
            }
        } else {
            throw new IllegalArgumentException("This launcher only can handle DockerSwarmComputer");
        }
    }

    private void launch(final DockerSwarmComputer computer, final TaskListener listener) throws IOException {
        final DockerSwarmCloud configuration = DockerSwarmCloud.get();
        final DockerSwarmAgentTemplate dockerSwarmAgentTemplate = configuration.getLabelConfiguration(this.label);

        this.agentInfo = this.bi.getAction(DockerSwarmAgentInfo.class);
        this.agentInfo.setDockerImage(dockerSwarmAgentTemplate.getImage());
        this.agentInfo.setLimitsNanoCPUs(dockerSwarmAgentTemplate.getLimitsNanoCPUs());
        this.agentInfo.setReservationsMemoryBytes(dockerSwarmAgentTemplate.getReservationsMemoryBytes());
        this.agentInfo.setReservationsNanoCPUs(dockerSwarmAgentTemplate.getReservationsNanoCPUs());

        setBaseWorkspaceLocation(dockerSwarmAgentTemplate);

        final String[] envVarOptions = dockerSwarmAgentTemplate.getEnvVarsConfig();
        final ArrayList<String> envVarsList = new ArrayList<>(Arrays.asList(envVarOptions));
        envVarsList.add("DOCKER_SWARM_PLUGIN_JENKINS_AGENT_SECRET=" + getAgentSecret(computer));
        envVarsList.add("DOCKER_SWARM_PLUGIN_JENKINS_URL=" + getJenkinsUrl(configuration));
        envVarsList.add("DOCKER_SWARM_PLUGIN_JENKINS_AGENT_JAR_URL=" + getAgentJarUrl(configuration));
        envVarsList.add("DOCKER_SWARM_PLUGIN_JENKINS_AGENT_JNLP_URL=" + getAgentJnlpUrl(computer, configuration));
        envVarsList.add("DOCKER_SWARM_PLUGIN_JENKINS_AGENT_NAME=" + getAgentName(computer));
        final String[] envVars = envVarsList.toArray(new String[0]);

        if (dockerSwarmAgentTemplate.isOsWindows()) {
            String command = dockerSwarmAgentTemplate.getWindowsCommand();
            HashMap<String,String> envHashMap = new HashMap<String,String>(){{
                // TODO derive automatically from above list
                put("\\%DOCKER_SWARM_PLUGIN_JENKINS_AGENT_SECRET\\%",getAgentSecret(computer));
                put("\\%DOCKER_SWARM_PLUGIN_JENKINS_URL\\%",getJenkinsUrl(configuration));
                put("\\%DOCKER_SWARM_PLUGIN_JENKINS_AGENT_JAR_URL\\%",getAgentJarUrl(configuration));
                put("\\%DOCKER_SWARM_PLUGIN_JENKINS_AGENT_JNLP_URL\\%",getAgentJnlpUrl(computer, configuration));
                put("\\%DOCKER_SWARM_PLUGIN_JENKINS_AGENT_NAME\\%",getAgentName(computer));
                
            }};
            for (String envVarString: envHashMap.keySet()){
                System.out.println("Replacing: "+ envVarString + " with: "+envHashMap.get(envVarString));
                command = command.replaceAll(envVarString, envHashMap.get(envVarString));
                System.out.println("Command: "+ command);
            }
            ArrayList<String> finalCommand = new ArrayList<String>();
                String [] windowsCommand = dockerSwarmAgentTemplate.getWindowsCommandConfig(command);
                for (String windowsCommandSegment: windowsCommand){
                    if(windowsCommandSegment.equals("${unixCommand}")){
                        for(String unixCommandSegment: dockerSwarmAgentTemplate.getUnixCommandConfig()){
                            finalCommand.add(unixCommandSegment);
                        }
                    } else{
                        finalCommand.add(windowsCommandSegment);
                    }
                }
            
            launchContainer(finalCommand.toArray(new String[0]), configuration, envVars, dockerSwarmAgentTemplate.getWorkingDir(),
                    dockerSwarmAgentTemplate.getUser(), dockerSwarmAgentTemplate, listener, computer,
                    dockerSwarmAgentTemplate.getHostsConfig());
        } else {
            launchContainer(dockerSwarmAgentTemplate.getUnixCommandConfig(), configuration, envVars,
                    dockerSwarmAgentTemplate.getWorkingDir(), dockerSwarmAgentTemplate.getUser(),
                    dockerSwarmAgentTemplate, listener, computer, dockerSwarmAgentTemplate.getHostsConfig());
        }
    }

    private void setBaseWorkspaceLocation(DockerSwarmAgentTemplate dockerSwarmAgentTemplate) {
        if (this.bi.task instanceof AbstractProject
                && StringUtils.isNotEmpty(dockerSwarmAgentTemplate.getBaseWorkspaceLocation())) {
            try {
                ((AbstractProject) this.bi.task)
                        .setCustomWorkspace(dockerSwarmAgentTemplate.getBaseWorkspaceLocation());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void launchContainer(String[] commands, DockerSwarmCloud configuration, String[] envVars, String dir,
            String user, DockerSwarmAgentTemplate dockerSwarmAgentTemplate, TaskListener listener,
            DockerSwarmComputer computer, String[] hosts) throws IOException {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        ServiceSpec crReq = createCreateServiceRequest(commands, configuration, envVars, dir, user,
                dockerSwarmAgentTemplate, computer, hosts);

        setLimitsAndReservations(dockerSwarmAgentTemplate, crReq);
        setHostBinds(dockerSwarmAgentTemplate, crReq);
        setHostNamedPipes(dockerSwarmAgentTemplate, crReq);
        setSecrets(dockerSwarmAgentTemplate, crReq);
        setConfigs(dockerSwarmAgentTemplate, crReq);
        setNetwork(configuration, crReq);
        setCacheDirs(configuration, dockerSwarmAgentTemplate, listener, computer, crReq);
        setVolumeDirs(configuration, dockerSwarmAgentTemplate, listener, computer, crReq);
        setTmpfs(dockerSwarmAgentTemplate, crReq);
        setPlacement(dockerSwarmAgentTemplate, crReq);
        setLabels(crReq);
        setRestartAttemptCount(crReq);
        setAuthHeaders(dockerSwarmAgentTemplate, crReq);
        setDnsIps(dockerSwarmAgentTemplate, crReq);
        setDnsSearchDomains(dockerSwarmAgentTemplate, crReq);
        setPortBinds(dockerSwarmAgentTemplate, crReq);
        setMetadata(dockerSwarmAgentTemplate, crReq);

        this.agentInfo.setServiceRequestJson(crReq.toJsonString());

        final ActorRef agentLauncher = swarmPlugin.getActorSystem()
                .actorOf(DockerSwarmAgentLauncherActor.props(listener.getLogger()), computer.getName());
        agentLauncher.tell(crReq, ActorRef.noSender());
    }

    private void setRestartAttemptCount(ServiceSpec crReq) {
        crReq.TaskTemplate.setRestartAttemptCount(0);
    }

    private void setLabels(ServiceSpec crReq) {
        crReq.addLabel("ROLE", "jenkins-agent");
    }

    private void setPlacement(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        crReq.TaskTemplate.setPlacement(dockerSwarmAgentTemplate.getPlacementConstraintsConfig(), 
            dockerSwarmAgentTemplate.getPlacementArchitecture(), dockerSwarmAgentTemplate.getPlacementOperatingSystem());
    }

    private ServiceSpec createCreateServiceRequest(String[] commands, DockerSwarmCloud configuration, String[] envVars,
            String dir, String user, DockerSwarmAgentTemplate dockerSwarmAgentTemplate, DockerSwarmComputer computer,
            String[] hosts) throws IOException {
        ServiceSpec crReq = new ServiceSpec(computer.getName(), dockerSwarmAgentTemplate.getImage(), commands, envVars,
                dir, user, hosts);
        return crReq;
    }

    private void setTmpfs(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        if (StringUtils.isNotEmpty(dockerSwarmAgentTemplate.getTmpfsDir())) {
            crReq.addTmpfsMount(dockerSwarmAgentTemplate.getTmpfsDir());
        }
    }

    private void setCacheDirs(DockerSwarmCloud configuration, DockerSwarmAgentTemplate dockerSwarmAgentTemplate,
            TaskListener listener, DockerSwarmComputer computer, ServiceSpec crReq) {
        final String[] cacheDirs = dockerSwarmAgentTemplate.getCacheDirs();
        if (cacheDirs.length > 0) {
            final String cacheVolumeName = getJobName() + "-" + computer.getVolumeName();
            this.bi.getAction(DockerSwarmAgentInfo.class).setCacheVolumeName(cacheVolumeName);
            for (int i = 0; i < cacheDirs.length; i++) {
                listener.getLogger().println("Binding Volume " + cacheVolumeName + " to " + cacheDirs[i]);
                crReq.addCacheVolume(cacheVolumeName, cacheDirs[i], configuration.getCacheDriverName());
            }
        }
    }

    private void setVolumeDirs(DockerSwarmCloud configuration, DockerSwarmAgentTemplate dockerSwarmAgentTemplate,
            TaskListener listener, DockerSwarmComputer computer, ServiceSpec crReq) {
        final String[] volumeDirs = dockerSwarmAgentTemplate.getVolumeDirs();
        if (volumeDirs.length > 0) {
            for (int i = 0; i < volumeDirs.length; i++) {
                final String cacheVolumeName = volumeDirs[i].split(":")[0];
                final String mountDir = volumeDirs[i].split(":")[1];
                listener.getLogger().println("Binding Volume " + cacheVolumeName + " to " + mountDir);
                crReq.addCacheVolume(cacheVolumeName, mountDir, configuration.getCacheDriverName());
            }
        }
    }

    private void setNetwork(DockerSwarmCloud configuration, ServiceSpec crReq) {
        crReq.setNetwork(configuration.getSwarmNetwork());
    }

    private void setHostBinds(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        String[] hostBinds = dockerSwarmAgentTemplate.getHostBindsConfig();
        for (int i = 0; i < hostBinds.length; i++) {
            String hostBind = hostBinds[i];
            String[] srcDest = hostBind.split(":");
            //on Windows machines with windows containers, you will likely have paths including the drive name,
            //e.g. "D:\host\dir:C:\container\dir" has 3 ":" - and should evaluate as addBindVolume("D:\host\dir","C:\container\dir")
            if (srcDest.length == 4){
                crReq.addBindVolume(srcDest[0]+":"+srcDest[1],srcDest[2]+":"+srcDest[3]);
            } else {
                crReq.addBindVolume(srcDest[0],srcDest[1]);
            }
        }
    }

    private void setHostNamedPipes(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        String[] hostNamedPipes = dockerSwarmAgentTemplate.getHostNamedPipesConfig();
        for (int i = 0; i < hostNamedPipes.length; i++) {
            String hostNamedPipe = hostNamedPipes[i];
            String[] srcDest = hostNamedPipe.split(":");
            crReq.addNamedPipeVolume(srcDest[0], srcDest[1]);
        }
    }

    private void setSecrets(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        String[] secrets = dockerSwarmAgentTemplate.getSecretsConfig();
        if (secrets.length > 0)
            try {
                final Object secretList = new ListSecretsRequest().execute();
                for (int i = 0; i < secrets.length; i++) {
                    String secret = secrets[i];
                    String[] split = secret.split(":");
                    boolean found = false;
                    for (Secret secretEntry : (List<Secret>) getResult(secretList, List.class)) {
                        if (secretEntry.Spec.Name.equals(split[0])) {
                            crReq.addSecret(secretEntry.ID, split[0], split[1]);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        LOGGER.log(Level.WARNING, "Secret {0} not found.", split[0]);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed setting secret", e);
            }
    }

    private void setConfigs(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        String[] configs = dockerSwarmAgentTemplate.getConfigsConfig();
        if (configs.length > 0)
            try {
                final Object configList = new ListConfigsRequest().execute();
                for (int i = 0; i < configs.length; i++) {
                    String config = configs[i];
                    String[] split = config.split(":");
                    boolean found = false;
                    for (Config configEntry : (List<Config>) getResult(configList, List.class)) {
                        if (configEntry.Spec.Name.equals(split[0])) {
                            crReq.addConfig(configEntry.ID, split[0], split[1]);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        LOGGER.log(Level.WARNING, "Config {0} not found.", split[0]);
                    }
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed setting config", e);
            }
    }

    private <T> T getResult(Object result, Class<T> clazz) {
        if (result instanceof SerializationException) {
            throw new RuntimeException(((SerializationException) result).getCause());
        }
        if (result instanceof ApiException) {
            throw new RuntimeException(((ApiException) result).getCause());
        }
        return clazz.cast(result);
    }

    private void setAuthHeaders(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        String credentialsId = dockerSwarmAgentTemplate.getPullCredentialsId();

        // Exit if no credentials are provided
        if (credentialsId == null || credentialsId.length() == 0) {
            return;
        }

        // Get the credentials
        StandardUsernamePasswordCredentials credentials = CredentialsMatchers
                .firstOrNull(lookupCredentials(StandardUsernamePasswordCredentials.class, (Item) null, ACL.SYSTEM,
                        Collections.<DomainRequirement>emptyList()), CredentialsMatchers.withId(credentialsId));

        // Add the credentials to the header
        crReq.setAuthHeader(credentials.getUsername(), credentials.getPassword().getPlainText(),
                dockerSwarmAgentTemplate.getEmail(), dockerSwarmAgentTemplate.getServerAddress());
    }

    private void setDnsIps(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        String[] dnsIps = dockerSwarmAgentTemplate.getDnsIpsConfig();
        for (String dnsIp : dnsIps) {
            crReq.addDnsIp(dnsIp);
        }
    }

    private void setDnsSearchDomains(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        String[] dnsSearchDomains = dockerSwarmAgentTemplate.getDnsSearchDomainsConfig();
        for (String dnsSearchDomain : dnsSearchDomains) {
            crReq.addDnsSearchDomain(dnsSearchDomain);
        }
    }

    private void setMetadata(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        String[] metadata = dockerSwarmAgentTemplate.getMetadataConfig();
        for(String mt : metadata) {
            if (!mt.contains("=")) {
                continue;
            }
            String[] metadataRecord = mt.split("=");
            if (metadataRecord.length == 2) {
                crReq.addLabel(metadataRecord[0], metadataRecord[1]);
            }
        }
    }

    private void setPortBinds(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        String[] portBinds = dockerSwarmAgentTemplate.getPortBindsConfig();
        for (String portBind : portBinds) {
            if (!portBind.contains(":")) {
                continue;
            }

            String[] srcDestProtocol = portBind.split("/");
            String[] srcDest = srcDestProtocol[0].split(":");
            crReq.addPortBind(srcDest[0], srcDest[1],
                    srcDestProtocol.length > 1 ? srcDestProtocol[1] : null);
        }
    }

    private void setLimitsAndReservations(DockerSwarmAgentTemplate dockerSwarmAgentTemplate, ServiceSpec crReq) {
        crReq.setTaskLimits(dockerSwarmAgentTemplate.getLimitsNanoCPUs(),
                dockerSwarmAgentTemplate.getLimitsMemoryBytes());
        crReq.setTaskReservations(dockerSwarmAgentTemplate.getReservationsNanoCPUs(),
                dockerSwarmAgentTemplate.getReservationsMemoryBytes());
    }

    private String getAgentJarUrl(final DockerSwarmCloud configuration) {
        return getJenkinsUrl(configuration) + "jnlpJars/agent.jar";
    }

    private String getAgentJnlpUrl(final Computer computer, final DockerSwarmCloud configuration) {
        return getJenkinsUrl(configuration) + computer.getUrl() + "jenkins-agent.jnlp";

    }

    private String getAgentSecret(final Computer computer) {
        return ((DockerSwarmComputer) computer).getJnlpMac();
    }

    private String getAgentName(final Computer computer) {
        return ((DockerSwarmComputer) computer).getName();
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
        return this.jobName.replaceAll("\\P{Alnum}", "_");
    }

}
