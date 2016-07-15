package suryagaddipati.jenkinsdockerslaves;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.google.common.base.Joiner;
import hudson.model.Run;
import jenkins.model.RunAction2;

import java.util.Date;
import java.util.List;

public class DockerSlaveInfo implements RunAction2 {



    private String cacheVolumeName;
    private String cacheVolumeNameMountPoint;
    private Integer maxMemoryUsage;
    private List<Long> perCpuUsage;

    public int getProvisioningAttempts() {
        return provisioningAttempts;
    }

    private  int provisioningAttempts;
    private  String containerId;
    private boolean provisioningInProgress;
    private Date  firstProvisioningAttempt;
    private Date provisionedTime;

    public DockerSlaveInfo(boolean provisioningInProgress) {
        this.provisioningInProgress = provisioningInProgress;
        firstProvisioningAttempt = new Date();
        provisioningAttempts = 1;
    }


    @Override
    public void onAttached(Run<?, ?> r) {

    }

    @Override
    public void onLoad(Run<?, ?> r) {

    }


    @Override
    public String getIconFileName() {
        return "/plugin/jenkins-docker-slaves/images/24x24/docker.png";
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


    public boolean isProvisioningInProgress() {
        return provisioningInProgress;
    }

    public void setProvisioningInProgress(boolean provisioningInProgress) {
        this.provisioningInProgress = provisioningInProgress;
    }

    public void setProvisionedTime(Date provisionedTime) {
        this.provisionedTime = provisionedTime;
    }

    public void incrementProvisioningAttemptCount() {
        provisioningAttempts++;
    }

    public void setContainerInfo(InspectContainerResponse containerInfo) {
        this.containerId =  containerInfo.getNode().getName() +containerInfo.getName();
    }


    public void setCacheVolumeName(String name) {
        this.cacheVolumeName = name;
    }

    public void setCacheVolumeMountpoint(String mountpoint) {
        this.cacheVolumeNameMountPoint = mountpoint;
    }

    public String getCacheVolumeName() {
        return cacheVolumeName;
    }

    public String getCacheVolumeNameMountPoint() {
        return cacheVolumeNameMountPoint;
    }

    public void setMaxMemoryUsage(Integer maxUsage) {
        this.maxMemoryUsage = maxUsage;
    }

    public Integer getMaxMemoryUsage() {
        return maxMemoryUsage;
    }
    public String getMemoryStats(){
       return maxMemoryUsage + " bytes (" + Math.floor((maxMemoryUsage/1024)/1024) +" MB )";
    }
    public String getCpuUsage(){
      return perCpuUsage == null? "": Joiner.on(", ").join(perCpuUsage);
    }

    public void setPerCpuUsage(List<Long> perCpuUsage) {
        this.perCpuUsage = perCpuUsage;
    }
}
