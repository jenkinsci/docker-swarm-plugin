package org.jenkinsci.plugins.docker.swarm;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.commons.credentials.DockerRegistryEndpoint;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerSwarmAgentTemplate implements Describable<DockerSwarmAgentTemplate> {
    private String tmpfsDir;
    private long limitsNanoCPUs;
    private long limitsMemoryBytes;
    private long reservationsNanoCPUs;
    private long reservationsMemoryBytes;
    private String image;
    private String hostBinds;
    private String hostNamedPipes;
    private String secrets;
    private String configs;
    private String dnsIps;
    private String dnsSearchDomains;
    private String portBinds;
    private String label;
    private boolean osWindows;
    private String command;
    private String user;
    private String workingDir;
    private String metadata;
    private String hosts;

    private String cacheDir;
    private String envVars;
    private String baseWorkspaceLocation;
    private String placementConstraints;
    private String email;
    private String serverAddress;
    private String pullCredentialsId;
    private String placementArchitecture;
    private String placementOperatingSystem;

    public DockerSwarmAgentTemplate() {
        // For Yaml Load
    }

    @DataBoundConstructor
    public DockerSwarmAgentTemplate(final String image, final String hostBinds, final String hostNamedPipes, final String dnsIps,
            final String dnsSearchDomains, final String command, final String user, final String workingDir,
            final String hosts, final String metadata, final String secrets, final String configs, final String label, final String cacheDir,
            final String tmpfsDir, final String envVars, final long limitsNanoCPUs, final long limitsMemoryBytes,
            final long reservationsNanoCPUs, final long reservationsMemoryBytes, String portBinds, final boolean osWindows,
            final String baseWorkspaceLocation, final String placementConstraints, final String placementArchitecture,
            final String placementOperatingSystem, final String email, final String serverAddress, final String pullCredentialsId) {
        this.image = image;
        this.hostBinds = hostBinds;
        this.hostNamedPipes = hostNamedPipes;
        this.dnsIps = dnsIps;
        this.dnsSearchDomains = dnsSearchDomains;
        this.command = command;
        this.user = user;
        this.workingDir = workingDir;
        this.hosts = hosts;
        this.metadata = metadata;
        this.secrets = secrets;
        this.configs = configs;
        this.label = label;
        this.cacheDir = cacheDir;
        this.tmpfsDir = tmpfsDir;
        this.limitsNanoCPUs = limitsNanoCPUs;
        this.limitsMemoryBytes = limitsMemoryBytes;
        this.reservationsNanoCPUs = reservationsNanoCPUs;
        this.reservationsMemoryBytes = reservationsMemoryBytes;
        this.envVars = envVars;
        this.portBinds = portBinds;
        this.osWindows = osWindows;
        this.baseWorkspaceLocation = baseWorkspaceLocation;
        this.placementConstraints = placementConstraints;
        this.placementArchitecture = placementArchitecture;
        this.placementOperatingSystem = placementOperatingSystem;
        this.email = email;
        this.serverAddress = serverAddress;
        this.pullCredentialsId = pullCredentialsId;
    }

    public String[] getCacheDirs() {
        return StringUtils.isEmpty(this.cacheDir) ? new String[] {} : this.cacheDir.split("[\\r\\n ]+");
    }

    public String getLabel() {
        return this.label;
    }

    public String[] getLabels() {
        return StringUtils.isEmpty(this.label) ? new String[]{} : this.label.split("\\s+");
    }

    public String getImage() {
        return this.image;
    }

    public String[] getHostBindsConfig() {
        return StringUtils.isEmpty(this.hostBinds) ? new String[] {} : this.hostBinds.split("[\\r\\n ]+");
    }

    public String[] getMetadataConfig() {
        return StringUtils.isEmpty(this.metadata) ? new String[] {} : this.metadata.split("[\\r\\n ]+");
    }

    public String[] getHostNamedPipesConfig() {
        return StringUtils.isEmpty(this.hostNamedPipes) ? new String[] {} : this.hostNamedPipes.split("[\\r\\n ]+");
    }

    public String[] getSecretsConfig() {
        return StringUtils.isEmpty(this.secrets) ? new String[] {} : this.secrets.split("[\\r\\n ]+");
    }

    public String[] getConfigsConfig() {
        return StringUtils.isEmpty(this.configs) ? new String[] {} : this.configs.split("[\\r\\n ]+");
    }

    public String[] getDnsIpsConfig() {
        return StringUtils.isEmpty(this.dnsIps) ? new String[] {} : this.dnsIps.split("[\\r\\n ]+");
    }

    public String[] getDnsSearchDomainsConfig() {
        return StringUtils.isEmpty(this.dnsSearchDomains) ? new String[] {} : this.dnsSearchDomains.split("[\\r\\n ]+");
    }

    public String[] getPortBindsConfig() {
        return StringUtils.isEmpty(this.portBinds) ? new String[]{} : this.portBinds.split("[\\r\\n ]+");
    }

    public String[] getEnvVarsConfig() {
        return StringUtils.isEmpty(this.envVars) ? new String[] {} : this.envVars.split("[\\r\\n]+");
    }

    public String[] getCommandConfig() {
        return StringUtils.isEmpty(this.command) ? new String[] {} : this.command.split("[\\r\\n]+");
    }

    public String[] getHostsConfig() {
        return StringUtils.isEmpty(this.hosts) ? new String[] {} : this.hosts.split("[\\r\\n]+");
    }

    public long getLimitsNanoCPUs() {
        return limitsNanoCPUs;
    }

    public long getLimitsMemoryBytes() {
        return limitsMemoryBytes;
    }

    public long getReservationsNanoCPUs() {
        return reservationsNanoCPUs;
    }

    public long getReservationsMemoryBytes() {
        return reservationsMemoryBytes;
    }

    public String getPlacementConstraints() {
        return placementConstraints;
    }

    public String getPlacementArchitecture() {
        return placementArchitecture;
    }

    public String getPlacementOperatingSystem() {
        return placementOperatingSystem;
    }

    public String getTmpfsDir() {
        return tmpfsDir;
    }

    public String getEnvVars() {
        return envVars;
    }

    @Override
    public Descriptor<DockerSwarmAgentTemplate> getDescriptor() {
        return (DescriptorImpl) Jenkins.getInstance().getDescriptor(getClass());
    }

    public String getBaseWorkspaceLocation() {
        return this.baseWorkspaceLocation;
    }

    public String[] getPlacementConstraintsConfig() {
        return StringUtils.isEmpty(this.placementConstraints) ? new String[] {} : this.placementConstraints.split(";");
    }

    public boolean isOsWindows() {
        return osWindows;
    }

    public String getWorkingDir() {
        return workingDir == null ? "/home/jenkins" : workingDir;
    }

    public String getPortBinds() {
        return portBinds;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<DockerSwarmAgentTemplate> {
        @Override
        public String getDisplayName() {
            return "Docker Agent Template";
        }

        public ListBoxModel doFillPullCredentialsIdItems(@AncestorInPath Item item,
                @QueryParameter String pullCredentialsId) {
            if (item == null && !Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)
                    || item != null && !item.hasPermission(Item.EXTENDED_READ)) {
                return new StandardListBoxModel();
            }

            final DockerRegistryEndpoint.DescriptorImpl descriptor = (DockerRegistryEndpoint.DescriptorImpl) Jenkins
                    .getInstance().getDescriptorOrDie(DockerRegistryEndpoint.class);
            return descriptor.doFillCredentialsIdItems(item);
        }
    }

    public String getHostBinds() {
        return hostBinds;
    }

    public String getMetadata() { return metadata; }

    public String getHostNamedPipes() {
        return hostNamedPipes;
    }

    public String getSecrets() {
        return secrets;
    }

    public String getConfigs() {
        return configs;
    }

    public String getCommand() {
        return command;
    }

    public String getHosts() {
        return hosts;
    }

    public String getDnsIps() {
        return dnsIps;
    }

    public String getDnsSearchDomains() {
        return dnsSearchDomains;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public String getUser() {
        return user;
    }

    public String getEmail() {
        return email;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getPullCredentialsId() {
        return pullCredentialsId;
    }

    public boolean hasLabel(String label) {
        return Arrays.asList(this.getLabels()).contains(label);
    }

}
