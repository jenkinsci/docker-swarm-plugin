package suryagaddipati.jenkinsdockerslaves;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class LabelConfiguration {
    private String network;
    String image;
    String hostBinds;
    String envVars;
    private Long  maxCpuShares;
    private Long maxMemory;
    private boolean dynamicResourceAllocation;
    private String label;
    private String cacheDir;

    public  LabelConfiguration(){
        //For Yaml Load
    }

    @DataBoundConstructor
    public LabelConfiguration(final String image, final String hostBinds, final String label, final String cacheDir, final Long maxCpuShares, final Long maxMemory, final boolean dynamicResourceAllocation, final String envVars, final String network) {
        this.image = image;
        this.hostBinds = hostBinds;
        this.label = label;
        this.cacheDir = cacheDir;
        this.maxCpuShares = maxCpuShares;
        this.maxMemory = maxMemory;
        this.dynamicResourceAllocation = dynamicResourceAllocation;
        this.envVars = envVars;
        this.network = network;
    }

    public String getNetwork() {
        return this.network;
    }

    public String getCacheDir() {
        return this.cacheDir;
    }

    public void setCacheDir(final String cacheDir) {
        this.cacheDir = cacheDir;
    }

    public String[] getCacheDirs() {
        return StringUtils.isEmpty(this.cacheDir) ? new String[]{} : this.cacheDir.split(" ");
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public String getHostBinds() {
        return this.hostBinds;
    }

    public void setHostBinds(final String hostBinds) {
        this.hostBinds = hostBinds;
    }

    public String getImage() {
        return this.image;
    }

    public void setImage(final String image) {
        this.image = image;
    }

    public String[] getHostBindsConfig() {
        return StringUtils.isEmpty(this.hostBinds) ? new String[]{} : this.hostBinds.split(" ");
    }


    public Long getMaxCpuShares() {
        return this.maxCpuShares ;
    }

    public void setMaxCpuShares(final Long maxCpuShares) {
        this.maxCpuShares = maxCpuShares;
    }

    public Long getMaxMemory() {
        return this.maxMemory;
    }

    public void setMaxMemory(final Long maxMemory) {
        this.maxMemory = maxMemory;
    }

    public boolean isDynamicResourceAllocation() {
        return this.dynamicResourceAllocation;
    }

    public void setDynamicResourceAllocation(final boolean dynamicResourceAllocation) {
        this.dynamicResourceAllocation = dynamicResourceAllocation;
    }

    public String getEnvVars() {
        return this.envVars;
    }

    public void setEnvVars(final String envVars) {
        this.envVars = envVars;
    }

    public String[] getEnvVarsConfig() {
        return StringUtils.isEmpty(this.envVars) ? new String[]{} : this.envVars.split(" ");
    }
}
