package suryagaddipati.jenkinsdockerslaves;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import hudson.model.Run;
import jenkins.model.RunAction2;

public class DockerSlaveInfo implements RunAction2 {
    private final String containerId;


    private final String containerName;

    public DockerSlaveInfo(InspectContainerResponse containerResponse) {
        this.containerId = containerResponse.getId();
        containerName = containerResponse.getName();
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
    public String getContainerName() {
        return containerName;
    }
}
