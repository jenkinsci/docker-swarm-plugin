package suryagaddipati.jenkinsdockerslaves;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public   class LabelConfiguration {
    String image;
    String hostBinds;
    String envVars;
    private Integer maxCpuShares;
    private Long maxMemory;


    private boolean dynamicResourceAllocation;

    @DataBoundConstructor
    public LabelConfiguration(String image, String hostBinds, String label, String cacheDir, Integer maxCpuShares, Long maxMemory, boolean dynamicResourceAllocation, String envVars) {
        this.image = image;
        this.hostBinds = hostBinds;
        this.label = label;
        this.cacheDir = cacheDir;
        this.maxCpuShares = maxCpuShares;
        this.maxMemory = maxMemory;
        this.dynamicResourceAllocation = dynamicResourceAllocation;
        this.envVars = envVars;
    }

    private String label;

    public String getCacheDir() {
        return cacheDir;
    }
    public String[] getCacheDirs() {
        return  StringUtils.isEmpty(cacheDir)? new String[]{}: cacheDir.split(" ");
    }

    public void setCacheDir(String cacheDir) {
        this.cacheDir = cacheDir;
    }

    private String cacheDir;


    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
    public void setHostBinds(String hostBinds) {
        this.hostBinds = hostBinds;
    }

    public void setImage(String image) {
        this.image = image;
    }
    public String getHostBinds() {
        return hostBinds;
    }

    public String getImage() {
        return image;
    }
    public String[] getHostBindsConfig() {
        return StringUtils.isEmpty( this.hostBinds)? new String[]{}: this.hostBinds.split(" ");
    }


    public Integer getMaxCpuShares() {
        return maxCpuShares == null ? 1: maxCpuShares;
    }

    public void setMaxCpuShares(Integer maxCpuShares) {
        this.maxCpuShares = maxCpuShares;
    }

    public Long getMaxMemory() {
        return maxMemory == null? 0l: maxMemory;
    }

    public void setMaxMemory(Long maxMemory) {
        this.maxMemory = maxMemory;
    }

    public boolean isDynamicResourceAllocation() {
        return dynamicResourceAllocation;
    }

    public void setDynamicResourceAllocation(boolean dynamicResourceAllocation) {
        this.dynamicResourceAllocation = dynamicResourceAllocation;
    }

    public void setEnvVars(String envVars) {
        this.envVars = envVars;
    }

    public String getEnvVars() {
        return envVars;
    }

    public String[] getEnvVarsConfig() {
        return StringUtils.isEmpty(this.envVars) ? new String[]{} : this.envVars.split(" ");
    }
}
