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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
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

    public List<LabelConfiguration> getLabelConfigurations() {
        return labelConfigurations;
    }

    public void setLabelConfigurations(List<LabelConfiguration> labelConfigurations) {
        this.labelConfigurations = labelConfigurations;
    }

    private List<LabelConfiguration> labelConfigurations;


    public DockerSlaveConfiguration() {
        load();
        if(labelConfigurations == null){
            labelConfigurations = new ArrayList<LabelConfiguration>();
        }
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }

    public DockerClient newDockerClient(){
        if (Boolean.TRUE.equals(useTLS)) {
            DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(uri)
                    .withDockerTlsVerify(true)
                    .withDockerCertPath(certificatesPath)
                    .withApiVersion(apiVersion)
                    .build();
            return  DockerClientBuilder.getInstance(config).build();
        } else {
            DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(uri).build();
            return  DockerClientBuilder.getInstance(config).build();
        }
    }


    public String getUri() {
        return uri;
    }

    public Boolean getUseTLS() {
        return useTLS;
    }



    public String getCertificatesPath() {
        return certificatesPath;
    }
    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setUseTLS(Boolean useTLS) {
        this.useTLS = useTLS;
    }



    public void setCertificatesPath(String certificatesPath) {
        this.certificatesPath = certificatesPath;
    }
    public static DockerSlaveConfiguration get() {
        return GlobalConfiguration.all().get(DockerSlaveConfiguration.class);
    }

    public boolean isPrivileged() {
        return privileged;
    }
    public void setPrivileged(boolean privileged) {
        this.privileged = privileged;
    }

    public String getJenkinsUrl() {
        return jenkinsUrl;
    }

    public void setJenkinsUrl(String jenkinsUrl) {
        this.jenkinsUrl = jenkinsUrl;
    }



    public String getBaseWorkspaceLocation() {
        return baseWorkspaceLocation;
    }

    public void setBaseWorkspaceLocation(String baseWorkspaceLocation) {
        this.baseWorkspaceLocation = baseWorkspaceLocation;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public List<String> getLabels() {
        Iterable<String> labels = Iterables.transform(getLabelConfigurations(), new Function<LabelConfiguration, String>() {
            public String apply(LabelConfiguration labelConfiguration) {
                return labelConfiguration.getLabel();
            }
        });
        return Lists.newArrayList(labels);
    }

    public LabelConfiguration getLabelConfiguration(String label) {
        for(LabelConfiguration labelConfiguration : labelConfigurations){
            if (label.equals(labelConfiguration.getLabel())){
                return labelConfiguration;
            }
        }

        return null;

    }

    public boolean canProvision(Label label) {
        return getLabelConfiguration(label.getName()) != null;
    }
}
