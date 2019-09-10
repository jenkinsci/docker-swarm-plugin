package org.jenkinsci.plugins.docker.swarm;

import static com.cloudbees.plugins.credentials.CredentialsMatchers.firstOrNull;
import static com.cloudbees.plugins.credentials.CredentialsMatchers.withId;
import static com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.dockerjava.core.SSLConfig;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.jenkinsci.plugins.docker.commons.credentials.DockerServerCredentials;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerEndpoint;
import org.jenkinsci.plugins.docker.swarm.docker.api.ping.PingRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiError;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiException;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import akka.actor.ActorRef;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.Descriptor;
import hudson.model.ItemGroup;
import hudson.model.Label;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.slaves.Cloud;
import hudson.slaves.NodeProvisioner;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public class DockerSwarmCloud extends Cloud {
    private static final String DOCKER_SWARM_CLOUD_NAME = "Docker Swarm";
    private static final Logger LOGGER = Logger.getLogger(DockerSwarmCloud.class.getName());
    private String jenkinsUrl;
    private String swarmNetwork;
    private String cacheDriverName;
    private String tunnel;
    private List<DockerSwarmAgentTemplate> agentTemplates = new ArrayList<>();

    private DockerServerEndpoint dockerHost;

    @DataBoundConstructor
    public DockerSwarmCloud(DockerServerEndpoint dockerHost, String dockerSwarmApiUrl, String jenkinsUrl,
            String swarmNetwork, String cacheDriverName, String tunnel, List<DockerSwarmAgentTemplate> agentTemplates) {
        super(DOCKER_SWARM_CLOUD_NAME);
        this.jenkinsUrl = jenkinsUrl;
        this.swarmNetwork = swarmNetwork;
        this.cacheDriverName = cacheDriverName;
        this.tunnel = tunnel;
        this.agentTemplates = agentTemplates;
        this.dockerHost = dockerHost;
    }

    // for yaml serialization
    public DockerSwarmCloud() {
        super(DOCKER_SWARM_CLOUD_NAME);
    }

    @Override
    public Collection<NodeProvisioner.PlannedNode> provision(final Label label, final int excessWorkload) {
        return new ArrayList<>();
    }

    @Override
    public boolean canProvision(final Label label) {
        return getLabels().contains(label.getName());
    }

    public DockerServerEndpoint getDockerHost() {
        return dockerHost;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Cloud> {

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context, @QueryParameter String value) {
            AccessControlled ac = (context instanceof AccessControlled ? (AccessControlled) context
                    : Jenkins.getInstance());
            if (!ac.hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel().includeCurrentValue(value);
            }
            return new StandardListBoxModel().includeAs(ACL.SYSTEM, context, DockerServerCredentials.class,
                    Collections.<DomainRequirement>emptyList());
        }

        @Override
        public String getDisplayName() {
            return "Docker Swarm";
        }

        public FormValidation doCheckJenkinsUrl(@QueryParameter String jenkinsUrl) {
            try {
                new URL(jenkinsUrl);
                return FormValidation.ok();
            } catch (MalformedURLException e) {
                return FormValidation.error(e, "Needs valid http url");
            }
        }

        @RequirePOST
        public FormValidation doValidateTestDockerApiConnection(@QueryParameter("uri") String uri,
                @QueryParameter("credentialsId") String credentialsId) throws IOException {
            if (uri.endsWith("/")) {
                return FormValidation.error("URI must not have trailing /");
            }
            Object response = new PingRequest(uri).execute();
            if (response instanceof ApiException) {
                return FormValidation.error(((ApiException) response).getCause(),
                        "Couldn't ping docker api: " + uri + "/_ping");
            }
            if (response instanceof ApiError) {
                return FormValidation.error(((ApiError) response).getMessage());
            }
            return FormValidation.ok("Connection successful");
        }
    }

    public String getDockerSwarmApiUrl() {
        return dockerHost.getUri();
    }

    private static SSLConfig toSSlConfig(String credentialsId) {
        if (credentialsId == null)
            return null;

        DockerServerCredentials credentials = firstOrNull(lookupCredentials(DockerServerCredentials.class,
                Jenkins.getInstance(), ACL.SYSTEM, Collections.<DomainRequirement>emptyList()), withId(credentialsId));
        return credentials == null ? null : new DockerServerCredentialsSSLConfig(credentials);
    }

    public SSLContext getSSLContext() throws IOException {
        try {
            final SSLConfig sslConfig = toSSlConfig(dockerHost.getCredentialsId());
            if (sslConfig != null) {
                return sslConfig.getSSLContext();
            }
        } catch (Exception e) {
            throw new IOException("Failed to create SSL Config ", e);
        }
        return null;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public String getSwarmNetwork() {
        return swarmNetwork;
    }

    public String getCacheDriverName() {
        return cacheDriverName;
    }

    public String getTunnel() {
        return tunnel;
    }

    public List<DockerSwarmAgentTemplate> getAgentTemplates() {
        return agentTemplates;
    }

    public DockerSwarmAgentTemplate getLabelConfiguration(final String label) {
        for (final DockerSwarmAgentTemplate dockerSwarmAgentTemplate : this.agentTemplates) {
            if (label.equals(dockerSwarmAgentTemplate.getLabel())) {
                return dockerSwarmAgentTemplate;
            }
        }
        return null;
    }

    public List<String> getLabels() {
        final Iterable<String> labels = Iterables.transform(getAgentTemplates(),
                dockerSwarmAgentTemplate -> dockerSwarmAgentTemplate.getLabel());
        return Lists.newArrayList(labels);
    }

    public static DockerSwarmCloud get() {
        return (DockerSwarmCloud) Jenkins.getInstance().getCloud(DOCKER_SWARM_CLOUD_NAME);
    }

    public void save() {
        getDescriptor().save();
    }

    @Initializer(after = InitMilestone.JOB_LOADED)
    public static void initFromYaml() throws IOException {
        File configsDir = new File(Jenkins.getInstance().getRootDir(), "pluginConfigs");
        File swarmConfigYaml = new File(configsDir, "swarm.yml");
        if (swarmConfigYaml.exists()) {
            LOGGER.info("Configuring swarm plugin from " + swarmConfigYaml.getAbsolutePath());
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            try (InputStream in = new BufferedInputStream(new FileInputStream(swarmConfigYaml))) {
                DockerSwarmCloud configuration = mapper.readValue(in, DockerSwarmCloud.class);
                DockerSwarmCloud existingCloud = DockerSwarmCloud.get();
                if (existingCloud != null) {
                    Jenkins.getInstance().clouds.remove(existingCloud);
                }
                Jenkins.getInstance().clouds.add(configuration);
            }
        }
        scheduleReaperActor();
        scheduleResetStuckBuildsActor();
    }

    private static void scheduleResetStuckBuildsActor() {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        final ActorRef resetStuckBuildsActor = swarmPlugin.getActorSystem()
                .actorOf(ResetStuckBuildsInQueueActor.props(), "reset-stuck-builds-actor");
        resetStuckBuildsActor.tell("start", ActorRef.noSender());
    }

    private static void scheduleReaperActor() {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        final ActorRef deadAgentReaper = swarmPlugin.getActorSystem().actorOf(DeadAgentServiceReaperActor.props(),
                "dead-agentService-reaper");
        deadAgentReaper.tell("start", ActorRef.noSender());
    }
}
