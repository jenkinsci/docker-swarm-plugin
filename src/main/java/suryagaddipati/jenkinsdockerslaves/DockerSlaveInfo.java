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

    private final Date firstProvisioningAttempt;
    private String cacheVolumeName;
    private String cacheVolumeNameMountPoint;
    private Integer allocatedCPUShares;
    private Long allocatedMemory;
    private Long maxMemoryUsage;
    private List<Long> perCpuUsage;
    private Integer throttledTime;
    private transient Run<?, ?> run;
    private Date computerLaunchTime;
    private int provisioningAttempts;
    private String containerId;
    private String dockerImage;
    private boolean provisioningInProgress;
    private Date provisionedTime;

    public DockerSlaveInfo(final boolean provisioningInProgress) {
        this.provisioningInProgress = provisioningInProgress;
        this.firstProvisioningAttempt = new Date();
        this.provisioningAttempts = 1;
    }

    public int getProvisioningAttempts() {
        return this.provisioningAttempts;
    }

    @Override
    public void onAttached(final Run<?, ?> r) {
        this.run = r;
    }

    @Override
    public void onLoad(final Run<?, ?> r) {
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
        return this.containerId;
    }


    public boolean isProvisioningInProgress() {
        return this.provisioningInProgress;
    }

    public void setProvisioningInProgress(final boolean provisioningInProgress) {
        this.provisioningInProgress = provisioningInProgress;
    }

    public void setProvisionedTime(final Date provisionedTime) {
        this.provisionedTime = provisionedTime;
    }

    public void incrementProvisioningAttemptCount() {
        this.provisioningAttempts++;
    }

    public void setContainerInfo(final InspectContainerResponse containerInfo) {
        this.containerId = containerInfo.getNode().getName() + containerInfo.getName();
    }

    public void setCacheVolumeMountpoint(final String mountpoint) {
        this.cacheVolumeNameMountPoint = mountpoint;
    }

    public String getCacheVolumeName() {
        return this.cacheVolumeName;
    }

    public void setCacheVolumeName(final String name) {
        this.cacheVolumeName = name;
    }

    public String getCacheVolumeNameMountPoint() {
        return this.cacheVolumeNameMountPoint;
    }


    public Long getMaxMemoryUsage() {
        return this.maxMemoryUsage;
    }

    public String getMemoryStats() {
        return this.maxMemoryUsage != null ? this.maxMemoryUsage + " bytes (" + Math.floor((this.maxMemoryUsage / 1024) / 1024) + " MB )" : "";
    }

    private void setMemoryStats(final Map<String, Object> memoryStats) {
        final Object maxUsage = memoryStats.get("max_usage");
        if (maxUsage instanceof Integer) {
            this.maxMemoryUsage = ((Integer) maxUsage).longValue();
        } else {
            this.maxMemoryUsage = (Long) maxUsage;
        }
    }

    public String getPerCpuUsage() {
        return this.perCpuUsage == null ? "" : Joiner.on(", ").join(this.perCpuUsage);
    }

    public String getTotalCpuUsage() {
        if (this.perCpuUsage == null) {
            return null;
        }
        long sum = 0;
        for (final Long value : this.perCpuUsage) {
            sum = sum + value;
        }
        final long seconds = TimeUnit.SECONDS.convert(sum, TimeUnit.NANOSECONDS);

        final long mins = TimeUnit.MINUTES.convert(sum, TimeUnit.NANOSECONDS);
        return mins + " mins, " + (seconds - mins * 60) + " secs.";

    }

    public void setStats(final Statistics stats) {
        final Map<String, Object> memoryStats = stats.getMemoryStats();
        setMemoryStats(memoryStats);
        setCpuStats(stats);
    }

    private void setCpuStats(final Statistics stats) {
        final Map<String, Object> cpuStats = stats.getCpuStats();
        if (cpuStats != null) {
            final Map<String, Object> cpuUsage = (Map<String, Object>) cpuStats.get("cpu_usage");
            if (cpuUsage != null) {
                final List<Long> perCpuUsage = (List<Long>) cpuUsage.get("percpu_usage");
                if (perCpuUsage != null) {
                    this.perCpuUsage = perCpuUsage;
                }

            }

            final Map<String, Object> throttlingData = (Map<String, Object>) cpuStats.get("throttling_data");
            if (throttlingData != null) {
                this.throttledTime = (Integer) throttlingData.get("throttled_time");
            }

        }
    }

    public boolean wasThrottled() {
        return this.throttledTime != null && this.throttledTime > 0;
    }

    public Integer getCpuAllocation() {
        return this.allocatedCPUShares;
    }

    public String getMemoryReservationString() {
        return this.allocatedMemory != null ? this.allocatedMemory + " bytes (" + Math.floor((this.allocatedMemory / 1024) / 1024) + " MB )" : "N/A";
    }

    public Integer getAllocatedCPUShares() {
        return this.allocatedCPUShares;
    }

    public void setAllocatedCPUShares(final Integer allocatedCPUShares) {
        this.allocatedCPUShares = allocatedCPUShares;
    }

    public boolean wereCpusAllocated() {
        return this.allocatedCPUShares != null && this.allocatedCPUShares != 0;
    }

    public Integer getThrottledTime() {
        return this.throttledTime;
    }

    public Long getMemoryReservation() {
        return this.allocatedMemory;
    }

    public Integer getNextCpuAllocation() {
        if (this.allocatedCPUShares != null && this.allocatedCPUShares != 0) {
            return wasThrottled() ? this.allocatedCPUShares + 1 : this.allocatedCPUShares;
        }
        return 1;
    }

    public Long getNextMemoryAllocation() {
        return this.maxMemoryUsage == null ? 0l : this.maxMemoryUsage + Bytes.MB(500);
    }

    public void setAllocatedMemory(final Long allocatedMemory) {
        this.allocatedMemory = allocatedMemory;
    }


    public boolean isBuildFinished() throws IOException {
        return !this.run.isBuilding();
    }

    public boolean isBuildPausable() throws IOException {
        return this.containerId != null && isPausable();
    }

    public boolean isBuildUnPausable() throws IOException {
        return this.containerId != null && isUnPausable();
    }

    public void doUnPauseBuild(final StaplerRequest req, final StaplerResponse rsp) throws ServletException, IOException {
        togglePause(false);
        rsp.forwardToPreviousPage(req);
    }

    public void doPauseBuild(final StaplerRequest req, final StaplerResponse rsp) throws ServletException, IOException {
        togglePause(true);
        rsp.forwardToPreviousPage(req);
    }

    private void togglePause(final boolean pause) throws IOException {
        if (this.containerId != null) {
            if (pause) {
                pause();
            } else {
                unpause();
            }
        }
    }

    public void pause() throws IOException {
        try (DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            dockerClient.pauseContainerCmd(this.containerId).exec();

            final FileOutputStream logger = new FileOutputStream(this.run.getLogFile(), true);
            logger.write("Build Paused. Resume Docker Slave to resume build. \n".getBytes());
        }
    }

    public void unpause() throws IOException {
        try (DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            dockerClient.unpauseContainerCmd(this.containerId).exec();
        }
    }

    public boolean isPausable() throws IOException {
        try (DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            final InspectContainerResponse container = dockerClient.inspectContainerCmd(this.containerId).exec();
            return !container.getState().getPaused();
        }
    }

    public boolean isUnPausable() throws IOException {
        try (DockerClient dockerClient = DockerSlaveConfiguration.get().newDockerClient()) {
            final InspectContainerResponse container = dockerClient.inspectContainerCmd(this.containerId).exec();
            return container.getState().getPaused();
        }
    }

    public void setComputerLaunchTime(final Date computerLaunchTime) {
        this.computerLaunchTime = computerLaunchTime;
    }

    public boolean isComputerProvisioningStuck() {
        if (this.computerLaunchTime != null) {
            final Duration secondsSpentProvisioning = Duration.ofMillis(new Date().getTime() - this.computerLaunchTime.getTime());
            return secondsSpentProvisioning.toMinutes() > 2;
        }
        return false;
    }

    public String getDockerImage() {
        return this.dockerImage;
    }

    public void setDockerImage(final String dockerImage) {
        this.dockerImage = dockerImage;
    }

}
