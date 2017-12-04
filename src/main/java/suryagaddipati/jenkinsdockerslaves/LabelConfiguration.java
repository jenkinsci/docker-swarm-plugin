package suryagaddipati.jenkinsdockerslaves;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LabelConfiguration {
    private long limitsNanoCPUs;
    private long limitsMemoryBytes;
    private long reservationsNanoCPUs;
    private long reservationsMemoryBytes;
    String image;
    String hostBinds;
    private String label;
    private String cacheDir;

    private transient Long  maxCpuShares;
    private transient Long maxMemory;
    private transient boolean dynamicResourceAllocation;
    String envVars;
    private String network;

    public  LabelConfiguration(){
        //For Yaml Load
    }

    @DataBoundConstructor
    public LabelConfiguration(final String image, final String hostBinds,
                              final String label, final String cacheDir,
                              final long limitsNanoCPUs, final long limitsMemoryBytes,
                              final long reservationsNanoCPUs, final long reservationsMemoryBytes ) {
        this.image = image;
        this.hostBinds = hostBinds;
        this.label = label;
        this.cacheDir = cacheDir;
        this.limitsNanoCPUs = limitsNanoCPUs;
        this.limitsMemoryBytes = limitsMemoryBytes;
        this.reservationsNanoCPUs = reservationsNanoCPUs;
        this.reservationsMemoryBytes = reservationsMemoryBytes;
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


    public String[] getEnvVarsConfig() {
        return StringUtils.isEmpty(this.envVars) ? new String[]{} : this.envVars.split(" ");
    }
    public long getLimitsNanoCPUs() {
        return limitsNanoCPUs;
    }

    public long getLimitsMemoryBytes() {
        return limitsMemoryBytes;
    }

    public long getReservationsNanoCPUs() {
        return reservationsNanoCPUs;
    }

    public long getReservationsMemoryBytes() {
        return reservationsMemoryBytes;
    }
}
