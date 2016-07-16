package suryagaddipati.jenkinsdockerslaves;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectVolumeResponse;
import com.github.dockerjava.api.model.Statistics;
import com.google.common.base.Joiner;
import hudson.model.Run;
import jenkins.model.RunAction2;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DockerSlaveInfo implements RunAction2 {



    private String cacheVolumeName;
    private String cacheVolumeNameMountPoint;
    private Integer allocatedCPUShares;
    private Long memoryReservation;

    private Integer maxMemoryUsage;
    private List<Long> perCpuUsage;
    private Integer throttledTime;

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
        this.allocatedCPUShares = containerInfo.getHostConfig().getCpuShares();
        this.memoryReservation = containerInfo.getHostConfig().getMemoryReservation();
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


    public Integer getMaxMemoryUsage() {
        return maxMemoryUsage;
    }
    public String getMemoryStats(){
       return maxMemoryUsage + " bytes (" + Math.floor((maxMemoryUsage/1024)/1024) +" MB )";
    }
    public String getCpuUsage(){
      return perCpuUsage == null? "": Joiner.on(", ").join(perCpuUsage);
    }

    public void setStats(Statistics stats) {
        Map<String, Object> memoryStats = stats.getMemoryStats();
        setMemoryStats(memoryStats);
        setCpuStats(stats);
    }

    private void setMemoryStats(Map<String, Object> memoryStats) {
        Integer maxUsage = (Integer) memoryStats.get("max_usage");
        this.maxMemoryUsage = maxUsage;
    }

    private void setCpuStats(Statistics stats) {
        Map<String, Object> cpuStats = stats.getCpuStats();
        if(cpuStats != null ){
            Map<String, Object>  cpuUsage= (Map<String, Object>) cpuStats.get("cpu_usage");
            if(cpuUsage != null ){
                List<Long> perCpuUsage= (List<Long>) cpuUsage.get("percpu_usage");
                if(perCpuUsage != null){
                    this.perCpuUsage = perCpuUsage;
                }

            }

            Map<String, Object>  throttlingData= (Map<String, Object>) cpuStats.get("throttling_data");
            if(throttlingData != null){
                this.throttledTime = (Integer)throttlingData.get("throttled_time");
            }

        }
    }

}
