package suryagaddipati.jenkinsdockerslaves;

import com.google.common.base.Joiner;
import hudson.model.Run;
import jenkins.model.RunAction2;

import java.io.IOException;
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
    private Integer throttledTime;
    private transient Run<?, ?> run;
    private Date computerLaunchTime;
    private int provisioningAttempts;
    private String dockerImage;

    @Deprecated
    private String containerId;
    @Deprecated
    private boolean provisioningInProgress;
    @Deprecated
    private Date provisionedTime;
    @Deprecated
    private List<Long> perCpuUsage;

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




    public void setProvisionedTime(final Date provisionedTime) {
        this.provisionedTime = provisionedTime;
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


    public Long getNextMemoryAllocation() {
        return this.maxMemoryUsage == null ? 0l : this.maxMemoryUsage + Bytes.MB(500);
    }

    public void setAllocatedMemory(final Long allocatedMemory) {
        this.allocatedMemory = allocatedMemory;
    }


    public boolean isBuildFinished() throws IOException {
        return !this.run.isBuilding();
    }


    public void setComputerLaunchTime(final Date computerLaunchTime) {
        this.computerLaunchTime = computerLaunchTime;
    }



    public String getDockerImage() {
        return this.dockerImage;
    }

    public void setDockerImage(final String dockerImage) {
        this.dockerImage = dockerImage;
    }

}
