package suryagaddipati.jenkinsdockerslaves;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Statistics;
import com.google.common.base.Joiner;
import hudson.model.Run;
import jenkins.model.RunAction2;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DockerSlaveInfo implements RunAction2 {

    private String cacheVolumeName;
    private String cacheVolumeNameMountPoint;



    private Integer allocatedCPUShares;
    private Long allocatedMemory;

    private Long maxMemoryUsage;
    private List<Long> perCpuUsage;


    private Integer throttledTime;
    private transient Run<?,?> run;


    private Date computerLaunchTime;

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
        this.run = r;
    }

    @Override
    public void onLoad(Run<?, ?> r) {
        this.run = r;
    }


    @Override
    public String getIconFileName() {
        return "/plugin/jenkins-docker-slaves/images/24x24/docker.png";
    }

    @Override
    public String getDisplayName() {
        return "Docker Slave";
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


    public Long getMaxMemoryUsage() {
        return maxMemoryUsage;
    }
    public String getMemoryStats(){
        return maxMemoryUsage != null? maxMemoryUsage + " bytes (" + Math.floor((maxMemoryUsage/1024)/1024) +" MB )" : "";
    }

    public String getPerCpuUsage(){
        return perCpuUsage == null? "": Joiner.on(", ").join(perCpuUsage);
    }
    public String getTotalCpuUsage(){
        if(perCpuUsage == null){
            return  null;
        }
        long sum = 0;
        for(Long value : perCpuUsage){
            sum = sum + value;
        }
        long seconds = TimeUnit.SECONDS.convert(sum, TimeUnit.NANOSECONDS);

        long mins = TimeUnit.MINUTES.convert(sum, TimeUnit.NANOSECONDS);
        return mins +" mins, " + (seconds - mins*60) + " secs.";

    }

    public void setStats(Statistics stats) {
        Map<String, Object> memoryStats = stats.getMemoryStats();
        setMemoryStats(memoryStats);
        setCpuStats(stats);
    }

    private void setMemoryStats(Map<String, Object> memoryStats) {
        Object maxUsage =  memoryStats.get("max_usage");
        if(maxUsage instanceof Integer){
            this.maxMemoryUsage = ((Integer)maxUsage).longValue();
        }else {
            this.maxMemoryUsage = (Long) maxUsage;
        }
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

    public boolean wasThrottled() {
        return throttledTime != null && throttledTime >0;
    }

    public Integer getCpuAllocation() {
        return allocatedCPUShares;
    }

    public String getMemoryReservationString(){
        return  allocatedMemory !=null ? allocatedMemory + " bytes (" + Math.floor((allocatedMemory/1024)/1024) +" MB )": "N/A";
    }

    public Integer getAllocatedCPUShares() {
        return allocatedCPUShares;
    }

    public boolean wereCpusAllocated() {
        return allocatedCPUShares != null && allocatedCPUShares != 0;
    }
    public Integer getThrottledTime() {
        return throttledTime;
    }

    public Long getMemoryReservation() {
        return allocatedMemory;
    }

    public Integer getNextCpuAllocation() {
        if(allocatedCPUShares != null && allocatedCPUShares != 0) {
            return wasThrottled()?allocatedCPUShares+1: allocatedCPUShares;
        }
        return 1;
    }

    public Long getNextMemoryAllocation() {
        return maxMemoryUsage ==null? 0l : maxMemoryUsage+ Bytes.MB(500);
    }

    public void setAllocatedCPUShares(Integer allocatedCPUShares) {
        this.allocatedCPUShares = allocatedCPUShares;
    }

    public void setAllocatedMemory(Long allocatedMemory) {
        this.allocatedMemory = allocatedMemory;
    }


    public boolean isBuildFinished() throws IOException {
        return !this.run.isBuilding();
    }
    public boolean isBuildPausable() throws IOException {
        return containerId != null && isPausable();
    }

    public boolean isBuildUnPausable() throws IOException {
        return containerId != null && isUnPausable();
    }

    public void doUnPauseBuild(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        togglePause(false);
        rsp.forwardToPreviousPage(req);
    }
    public void doPauseBuild(StaplerRequest req, StaplerResponse rsp) throws ServletException, IOException {
        togglePause(true);
        rsp.forwardToPreviousPage(req);
    }

    private void togglePause(boolean pause) throws IOException {
        if(containerId != null){
            if(pause){
                pause();
            }else{
                unpause();
            }
        }
    }

    public  void pause() throws IOException {
        try(DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            dockerClient.pauseContainerCmd(containerId).exec();

            FileOutputStream logger = new FileOutputStream(run.getLogFile(),true);
            logger.write("Build Paused. Resume Docker Slave to resume build. \n".getBytes());
        }
    }
    public  void unpause() throws IOException {
        try(DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            dockerClient.unpauseContainerCmd(containerId).exec();
        }
    }

    public boolean isPausable() throws IOException {
        try(DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
            return !container.getState().getPaused();
        }
    }

    public boolean isUnPausable() throws IOException {
        try(DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            InspectContainerResponse container = dockerClient.inspectContainerCmd(containerId).exec();
            return container.getState().getPaused();
        }
    }

    public void setComputerLaunchTime(Date computerLaunchTime) {
        this.computerLaunchTime = computerLaunchTime;
    }

    public boolean isComputerProvisioningStuck(){
        if(computerLaunchTime != null){
            Duration secondsSpentProvisioning = Duration.ofMillis(new Date().getTime() - computerLaunchTime.getTime());
            return secondsSpentProvisioning.toMinutes() > 2;
        }
        return false;
    }
}
