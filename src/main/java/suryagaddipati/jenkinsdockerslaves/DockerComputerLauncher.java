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

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.DockerException;
import com.spotify.docker.client.ImageNotFoundException;
import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.ProgressMessage;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

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
        try {

            DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
            DockerClient docker = configuration.newDockerClient();

            final String additionalSlaveOptions = "-noReconnect";
            final String slaveOptions = "-jnlpUrl " + getSlaveJnlpUrl(computer,configuration) + " -secret " + getSlaveSecret(computer) + " " + additionalSlaveOptions;
            final String[] command = new String[] {"sh", "-c", "curl -o slave.jar " + getSlaveJarUrl(configuration) + " && java -jar slave.jar " + slaveOptions};
            final ContainerConfig.Builder containerConfigBuilder = ContainerConfig.builder().image(configuration.getImage()).cmd(command);
            final HostConfig.Builder hostConfigBuilder = HostConfig.builder();
            hostConfigBuilder.privileged(configuration.isPrivileged());
            hostConfigBuilder.binds(configuration.getHostBindsConfig());
            pullImageIfNotFound(docker,configuration.getImage(),listener.getLogger());
            ContainerCreation creation = docker.createContainer(containerConfigBuilder.build());
            docker.startContainer(creation.id(), hostConfigBuilder.build());
            docker.logs(creation.id(), DockerClient.LogsParameter.FOLLOW).attach(listener.getLogger(),listener.getLogger());
            listener.getLogger().print("Created container :" + creation.id() );
            computer.connect(false).get();

        } catch (Exception e) {
            e.printStackTrace(listener.getLogger());
        }
    }

    private void pullImageIfNotFound(DockerClient docker, String dockerImage, final PrintStream logger) throws DockerException, InterruptedException {
        boolean imageExists;
        try {
            logger.println("Checking if image " + dockerImage + " exists.");
            if (docker.inspectImage(dockerImage) != null) {
                imageExists = true;
            } else {
                // Should be unreachable.
                imageExists = false;
            }
        } catch (ImageNotFoundException e) {
            imageExists = false;
        }

        logger.println("Image " + dockerImage + " exists? " + imageExists );

        if (!imageExists ) {
            logger.println("Pulling image " + dockerImage + ".");
            docker.pull(dockerImage, new ProgressHandler(){
                @Override
                public void progress(ProgressMessage message) throws DockerException {
                    if(message.progress() != null)
                    logger.println(message.progress());
                }
            });
            logger.println("Finished pulling image " + dockerImage + ".");
        }

    }
    private String getJenkinsUrl(DockerSlaveConfiguration configuration) {
        String url = configuration.getJenkinsUrl();
        if (url.endsWith("/")) {
            return url;
        } else {
            return url + '/';
        }
    }
    private String getSlaveJarUrl(DockerSlaveConfiguration configuration) {
        return getJenkinsUrl(configuration) + "jnlpJars/slave.jar";
    }

    private String getSlaveJnlpUrl(Computer computer, DockerSlaveConfiguration configuration) {
        return getJenkinsUrl(configuration) + computer.getUrl() + "slave-agent.jnlp";

    }

    private String getSlaveSecret(Computer computer) {
        return ((DockerComputer)computer).getJnlpMac();

    }

}
