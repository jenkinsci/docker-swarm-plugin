/*
The MIT License (MIT)

Copyright (c) 2014, Groupon, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package suryagaddipati.jenkinsdockerslaves;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.Label;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

@Extension
public class DockerSlaveConfiguration extends GlobalConfiguration {
    String uri;
    Boolean useTLS;
    String certificatesPath;


    String apiVersion;
    private boolean privileged;
    private String jenkinsUrl;
    private String baseWorkspaceLocation;


    private String swarmNetwork;


    private int maxProvisioningAttempts;
    private List<LabelConfiguration> labelConfigurations = new ArrayList<>();

    public DockerSlaveConfiguration() {
        load();
    }

    public static DockerSlaveConfiguration get() {
        return GlobalConfiguration.all().get(DockerSlaveConfiguration.class);
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

    public void setUri(final String uri) {
        this.uri = uri;
    }

    public Boolean getUseTLS() {
        return this.useTLS;
    }

    public void setUseTLS(final Boolean useTLS) {
        this.useTLS = useTLS;
    }

    public String getCertificatesPath() {
        return this.certificatesPath;
    }

    public void setCertificatesPath(final String certificatesPath) {
        this.certificatesPath = certificatesPath;
    }

    public boolean isPrivileged() {
        return this.privileged;
    }

    public void setPrivileged(final boolean privileged) {
        this.privileged = privileged;
    }

    public String getJenkinsUrl() {
        return this.jenkinsUrl;
    }

    public void setJenkinsUrl(final String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }


    public String getBaseWorkspaceLocation() {
        return this.baseWorkspaceLocation;
    }

    public void setBaseWorkspaceLocation(final String baseWorkspaceLocation) {
        this.baseWorkspaceLocation = baseWorkspaceLocation;
    }

    public String getApiVersion() {
        return this.apiVersion;
    }

    public void setApiVersion(final String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public List<String> getLabels() {
        final Iterable<String> labels = Iterables.transform(getLabelConfigurations(), new Function<LabelConfiguration, String>() {
            public String apply(final LabelConfiguration labelConfiguration) {
                return labelConfiguration.getLabel();
            }
        });
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

    public boolean canProvision(final Label label) {
        return getLabelConfiguration(label.getName()) != null;
    }

    public int getMaxProvisioningAttempts() {
        return this.maxProvisioningAttempts;
    }

    public void setMaxProvisioningAttempts(final int maxProvisioningAttempts) {
        this.maxProvisioningAttempts = maxProvisioningAttempts;
    }

    public String getSwarmNetwork() {
        return swarmNetwork;
    }

    public void setSwarmNetwork(String swarmNetwork) {
        this.swarmNetwork = swarmNetwork;
    }
}
