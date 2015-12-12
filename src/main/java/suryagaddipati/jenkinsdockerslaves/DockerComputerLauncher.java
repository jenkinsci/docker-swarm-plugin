/*
 * The MIT License
 *
 *  Copyright (c) 2015, CloudBees, Inc.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 *
 */

package suryagaddipati.jenkinsdockerslaves;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import hudson.model.AbstractProject;
import hudson.model.Computer;
import hudson.model.FreeStyleBuild;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DockerComputerLauncher extends ComputerLauncher {


    @Override
    public void launch(final SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        if (computer instanceof DockerComputer) {
            launch((DockerComputer) computer, listener);
        } else {
            throw new IllegalArgumentException("This launcher only can handle DockerComputer");
        }
    }

    private void launch(final DockerComputer computer, TaskListener listener) throws IOException, InterruptedException {
        // we need to capture taskListener here, as it's a private field of Computer
        TeeTaskListener teeListener = computer.initTeeListener(listener);

        try {

            DockerClient docker = buildDockerClient("https://192.168.99.100:2376", true, "/Users/sgaddipati/.docker/machine/machines/docker");
//            DockerClient docker = buildDockerClient("http://10.20.90.34:9000", false, "/Users/sgaddipati/.docker/machine/machines/docker");

            final String additionalSlaveOptions = "-noReconnect";
            final String slaveOptions = "-jnlpUrl " + getSlaveJnlpUrl(computer) + " -secret " + getSlaveSecret(computer) + " " + additionalSlaveOptions;
            final String[] command = new String[] {"sh", "-c", "curl -o slave.jar " + getSlaveJarUrl() + " && java -jar slave.jar " + slaveOptions};
            final ContainerConfig.Builder containerConfigBuilder = ContainerConfig.builder().image("docker.groupondev.com/docker/jenkins_slave").cmd(command);
            final HostConfig.Builder hostConfigBuilder = HostConfig.builder();
            hostConfigBuilder.privileged(true);
            hostConfigBuilder.binds("/usr/local/bin/docker:/usr/local/bin/docker", "/var/run/docker.sock:/var/run/docker.sock");

            ContainerCreation creation = docker.createContainer(containerConfigBuilder.build());
            docker.startContainer(creation.id(), hostConfigBuilder.build());
//            computer.getSlave().set
            computer.connect(false).get();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String getJenkinsBaseUrl() {
        return "http://192.168.99.1:8080/jenkins/";
//            return "http://buildmaster2.snc1:8080/";
//        String url = JenkinsLocationConfiguration.get().getUrl();
//        if(url == null)
//        if (url.endsWith("/")) {
//            return url;
//        } else {
//            return url + '/';
//        }
    }

    /*
     * Get the slave jar URL.
     */
    private String getSlaveJarUrl() {
        return getJenkinsBaseUrl() + "jnlpJars/slave.jar";
    }

    /*
     * Get the JNLP URL for the slave.
     */
    private String getSlaveJnlpUrl(Computer computer) {
        return getJenkinsBaseUrl() + computer.getUrl() + "slave-agent.jnlp";

    }

    private String getSlaveSecret(Computer computer) {
        return ((DockerComputer)computer).getJnlpMac();

    }


    // -- A terrible hack; but we can't find a better way so far ---

    protected void recordFailureOnBuild(final DockerComputer computer, TeeTaskListener teeListener, IOException e) throws IOException, InterruptedException {
        final Job job = computer.getJob();
        Queue.Item queued = job.getQueueItem();
        Jenkins.getInstance().getQueue().cancel(queued);

        // DockerComputer can only be used with AbstractProject
        Run run = ((AbstractProject) job).createExecutable();
        writeFakeBuild(new File(run.getRootDir(),"build.xml"), run);
        writeLog(new File(run.getRootDir(),"log"), teeListener);
        run.reload();
    }


    private void writeTagBegin(OutputStream out, String tag) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(tag).append(">");
        out.write(builder.toString().getBytes(Charsets.UTF_8));
    }

    private void writeTagEnd(OutputStream out, String tag) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("</").append(tag).append(">\n");
        out.write(builder.toString().getBytes(Charsets.UTF_8));
    }

    private void writeTag(OutputStream out, String tag, String value) throws IOException {
        writeTagBegin(out, tag);
        StringBuilder builder = new StringBuilder();
        builder.append(value);
        out.write(builder.toString().getBytes(Charsets.UTF_8));
        writeTagEnd(out, tag);
    }

    private static final String XML_HEADER = "<?xml version='1.0' encoding='UTF-8'?>\n";

    private void writeFakeBuild(File file, Run run) throws IOException {
        String topLevelTag = run instanceof FreeStyleBuild ? "build" : run.getClass().getCanonicalName();

        FileOutputStream fos = new FileOutputStream(file);
        try {
            fos.write(XML_HEADER.getBytes(Charsets.UTF_8));
            writeTagBegin(fos, topLevelTag);
            writeTag(fos, "timestamp", Long.toString(System.currentTimeMillis()));
            writeTag(fos, "startTime", Long.toString(System.currentTimeMillis()));
            writeTag(fos, "result", Result.NOT_BUILT.toString());
            writeTag(fos, "duration", "0");
            writeTag(fos, "keepLog", "false");
            writeTagEnd(fos, topLevelTag);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private void writeLog(File file, TeeTaskListener teeListener) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        try {
            teeListener.setSideOutputStream(fos);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }
    private DockerClient buildDockerClient(String uri, Boolean useTLS, String certificatesPath) throws Exception {

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

}
