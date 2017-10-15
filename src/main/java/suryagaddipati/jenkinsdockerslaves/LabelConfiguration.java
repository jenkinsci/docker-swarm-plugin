package suryagaddipati.jenkinsdockerslaves;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

public class LabelConfiguration {
    private String network;
    private Long limitsNanoCPUs;
    private Long limitsMemoryBytes;
    private Long reservationsNanoCPUs;
    private Long reservationsMemoryBytes;
    String image;
    String hostBinds;
    String envVars;
    private String label;
    private String cacheDir;

    private transient Long  maxCpuShares;
    private transient Long maxMemory;
    private transient boolean dynamicResourceAllocation;

    public  LabelConfiguration(){
        //For Yaml Load
    }

    @DataBoundConstructor
    public LabelConfiguration(final String image, final String hostBinds,
                              final String label, final String cacheDir,
                              final String envVars, final String network,
                              final Long limitsNanoCPUs, final Long limitsMemoryBytes,
                              final Long reservationsNanoCPUs, final Long reservationsMemoryBytes ) {
        this.image = image;
        this.hostBinds = hostBinds;
        this.label = label;
        this.cacheDir = cacheDir;
        this.envVars = envVars;
        this.network = network;
        this.limitsNanoCPUs = limitsNanoCPUs;
        this.limitsMemoryBytes = limitsMemoryBytes;
        this.reservationsNanoCPUs = reservationsNanoCPUs;
        this.reservationsMemoryBytes = reservationsMemoryBytes;
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

    public String getHostBinds() {
        return this.hostBinds;
    }
    public String getImage() {
        return this.image;
    }

    public String[] getHostBindsConfig() {
        return StringUtils.isEmpty(this.hostBinds) ? new String[]{} : this.hostBinds.split(" ");
    }

    public boolean isDynamicResourceAllocation() {
        return this.dynamicResourceAllocation;
    }

    public String getEnvVars() {
        return this.envVars;
    }


    public String[] getEnvVarsConfig() {
        return StringUtils.isEmpty(this.envVars) ? new String[]{} : this.envVars.split(" ");
    }
    public Long getLimitsNanoCPUs() {
        return limitsNanoCPUs;
    }

    public Long getLimitsMemoryBytes() {
        return limitsMemoryBytes;
    }

    public Long getReservationsNanoCPUs() {
        return reservationsNanoCPUs;
    }

    public Long getReservationsMemoryBytes() {
        return reservationsMemoryBytes;
    }
}
