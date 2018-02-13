package org.jenkinsci.plugins.docker.swarm;

import hudson.model.Run;
import jenkins.model.RunAction2;

public class DockerSwarmAgentInfo implements RunAction2 {

    private String cacheVolumeName;
    private String dockerImage;
    private boolean provisioningInProgress;


    public DockerSwarmAgentInfo(final boolean provisioningInProgress) {
        this.provisioningInProgress = provisioningInProgress;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/docker-swarm-plugin/images/24x24/docker.png";
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

    @Override
    public void onAttached(Run<?, ?> run) {

    }

    @Override
    public void onLoad(Run<?, ?> run) {

    }
}
