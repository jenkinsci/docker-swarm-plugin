package org.jenkinsci.plugins.docker.swarm;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class DockerSwarmAgentInfo implements RunAction2 {

    private String cacheVolumeName;
    private String dockerImage;
    private boolean provisioningInProgress;
    private long limitsNanoCPUs;
    private long reservationsNanoCPUs;
    private long reservationsMemoryBytes;
    private String agentLabel;
    private String serviceRequestJson;

    public DockerSwarmAgentInfo(final boolean provisioningInProgress) {
        this.provisioningInProgress = provisioningInProgress;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/docker-swarm/images/24x24/docker.png";
    }

    @Override
    public String getDisplayName() {
        return "Docker Swarm Agent";
    }

    @Override
    public String getUrlName() {
        return "dockerSwarmAgentInfo";
    }

    public boolean isProvisioningInProgress() {
        return provisioningInProgress;
    }

    public String getCacheVolumeName() {
        return this.cacheVolumeName;
    }

    public void setCacheVolumeName(final String name) {
        this.cacheVolumeName = name;
    }

    public String getDockerImage() {
        return this.dockerImage;
    }

    public void setDockerImage(final String dockerImage) {
        this.dockerImage = dockerImage;
    }

    public long getLimitsNanoCPUs() {
        return this.limitsNanoCPUs;
    }

    public void setLimitsNanoCPUs(final long limitsNanoCPUs) {
        this.limitsNanoCPUs = limitsNanoCPUs;
    }

    public long getReservationsNanoCPUs() {
        return this.reservationsNanoCPUs;
    }

    public void setReservationsNanoCPUs(final long reservationsNanoCPUs) {
        this.reservationsNanoCPUs = reservationsNanoCPUs;
    }

    public long getReservationsMemoryBytes() {
        return this.reservationsMemoryBytes;
    }

    public void setReservationsMemoryBytes(final long reservationsMemoryBytes) {
        this.reservationsMemoryBytes = reservationsMemoryBytes;
    }

    public String getAgentLabel() {
        return this.agentLabel;
    }

    public void setAgentLabel(final String agentLabel) {
        this.agentLabel = agentLabel;
    }

    public String getServiceRequestJson() {
        return serviceRequestJson;
    }

    public void setServiceRequestJson(String serviceRequestJson) {
        this.serviceRequestJson = serviceRequestJson;
    }

    @Override
    public void onAttached(Run<?, ?> run) {

    }

    @Override
    public void onLoad(Run<?, ?> run) {

    }

}
