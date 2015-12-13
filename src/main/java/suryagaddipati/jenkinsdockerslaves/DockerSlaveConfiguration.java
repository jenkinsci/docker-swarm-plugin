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

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificateException;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import hudson.Extension;
import hudson.model.Node;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

@Extension
public class DockerSlaveConfiguration extends GlobalConfiguration {
    String uri;
    Boolean useTLS;
    String certificatesPath;

    String image;
    String hostBinds;



    private boolean privileged;
    private String jenkinsUrl;
    private String label;


    public DockerSlaveConfiguration() {
        load();
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


    public String[] getHostBindsConfig() {
        return this.hostBinds.split(" ");
    }

    public String getUri() {
        return uri;
    }

    public Boolean getUseTLS() {
        return useTLS;
    }

    public String getHostBinds() {
        return hostBinds;
    }

    public String getImage() {
        return image;
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

    public void setHostBinds(String hostBinds) {
        this.hostBinds = hostBinds;
    }

    public void setImage(String image) {
        this.image = image;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
