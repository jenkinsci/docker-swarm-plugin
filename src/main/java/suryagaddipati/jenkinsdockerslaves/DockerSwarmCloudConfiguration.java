package suryagaddipati.jenkinsdockerslaves;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

@Extension
public class DockerSwarmCloudConfiguration extends GlobalConfiguration {
    String uri;


    private String jenkinsUrl;


    private String swarmNetwork;
    private String cacheDriverName;


    private List<LabelConfiguration> labelConfigurations = new ArrayList<>();

    public DockerSwarmCloudConfiguration() {
        load();
    }

    public static DockerSwarmCloudConfiguration get() {
        return GlobalConfiguration.all().get(DockerSwarmCloudConfiguration.class);
    }

    public List<LabelConfiguration> getLabelConfigurations() {
        return this.labelConfigurations;
    }

    public void setLabelConfigurations(final List<LabelConfiguration> labelConfigurations) {
        this.labelConfigurations = labelConfigurations;
    }

    @Override
    public boolean configure(final StaplerRequest req, final JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }



    public String getUri() {
        return this.uri;
    }
    public String getDockerUri(){
        return getUri();
    }

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public String getJenkinsUrl() {
        return this.jenkinsUrl;
    }

    public void setJenkinsUrl(final String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }


    public List<String> getLabels() {
        final Iterable<String> labels = Iterables.transform(getLabelConfigurations(), labelConfiguration -> labelConfiguration.getLabel());
        return Lists.newArrayList(labels);
    }

    public LabelConfiguration getLabelConfiguration(final String label) {
        for (final LabelConfiguration labelConfiguration : this.labelConfigurations) {
            if (label.equals(labelConfiguration.getLabel())) {
                return labelConfiguration;
            }
        }

        return null;

    }


    public String getSwarmNetwork() {
        return swarmNetwork;
    }

    public void setSwarmNetwork(String swarmNetwork) {
        this.swarmNetwork = swarmNetwork;
    }

    public String getCacheDriverName() {
        return cacheDriverName;
    }

    public void setCacheDriverName(String cacheDriverName) {
        this.cacheDriverName = cacheDriverName;
    }
}
