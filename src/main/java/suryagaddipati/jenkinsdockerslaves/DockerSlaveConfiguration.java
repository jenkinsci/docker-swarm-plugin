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
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import hudson.Extension;
import hudson.model.Computer;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nullable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Extension
public class DockerSlaveConfiguration extends GlobalConfiguration {
    String uri;
    Boolean useTLS;
    String certificatesPath;




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

    public DockerClient newDockerClient() throws DockerCertificateException {
        final URI dockerUri = URI.create(uri);

        DockerClient dockerClient;
        if (Boolean.TRUE.equals(useTLS)) {
            final Path certsPath = Paths.get(certificatesPath);
            final DockerCertificates dockerCerts = new DockerCertificates(certsPath);
            dockerClient = new DefaultDockerClient(dockerUri, dockerCerts);
        } else {
            dockerClient = new DefaultDockerClient(dockerUri);
        }
        return dockerClient;
    }


    @Override
    public synchronized void save() {
        super.save();
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
}
