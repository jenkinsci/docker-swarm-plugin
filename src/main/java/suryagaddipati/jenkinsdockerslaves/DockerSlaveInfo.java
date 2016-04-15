package suryagaddipati.jenkinsdockerslaves;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class DockerSlaveInfo implements RunAction2 {
    private  String containerId;
    private boolean provisioningInProgress;

    public DockerSlaveInfo(boolean provisioningInProgress) {
        this.provisioningInProgress = provisioningInProgress;
    }


    @Override
    public void onAttached(Run<?, ?> r) {

    }

    @Override
    public void onLoad(Run<?, ?> r) {

    }


    @Override
    public String getIconFileName() {
        return "new-package.png";
    }

    @Override
    public String getDisplayName() {
        return "Docker Slave Info";
    }

    @Override
    public String getUrlName() {
        return "dockerSlaveInfo";
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(CreateContainerResponse container) {
        this.containerId = container.getId();
    }

    public boolean isProvisioningInProgress() {
        return provisioningInProgress;
    }

    public void setProvisioningInProgress(boolean provisioningInProgress) {
        this.provisioningInProgress = provisioningInProgress;
    }
}
