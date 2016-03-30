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


import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.io.PrintStream;

public class DockerComputerLauncher extends ComputerLauncher {


    private String label;

    public DockerComputerLauncher(String label) {

        this.label = label;
    }

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
            DockerClient dockerClient = configuration.newDockerClient();
            LabelConfiguration labelConfiguration = configuration.getLabelConfiguration(this.label);


            final String additionalSlaveOptions = "-noReconnect";
            final String slaveOptions = "-jnlpUrl " + getSlaveJnlpUrl(computer,configuration) + " -secret " + getSlaveSecret(computer) + " " + additionalSlaveOptions;
            final String[] command = new String[] {"sh", "-c", "curl -o slave.jar " + getSlaveJarUrl(configuration) + " && java -jar slave.jar " + slaveOptions};

            CreateContainerCmd containerCmd = dockerClient
                    .createContainerCmd(labelConfiguration.getImage())
                    .withCmd(command)
                    .withPrivileged(configuration.isPrivileged())
                    .withName(computer.getName());

            String[] bindOptions = labelConfiguration.getHostBindsConfig();
            if(bindOptions.length != 0){
                Bind[]  binds = new Bind[bindOptions.length];
                for(int i = 0; i < bindOptions.length ; i++){
                    String[] bindConfig = bindOptions[i].split(":");
                   binds[i] = new Bind(bindConfig[0], new Volume(bindConfig[1]));
                }
                containerCmd.withBinds(binds);
            }



            CreateContainerResponse container = containerCmd.exec();
            listener.getLogger().print("Created container :" + container.getId() );
            dockerClient.startContainerCmd(container.getId()) .exec();
            computer.connect(false).get();

        } catch (Exception e) {
            throw new RuntimeException(e);
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

    private String getJenkinsUrl(DockerSlaveConfiguration configuration) {
        String url = configuration.getJenkinsUrl();
        if (url.endsWith("/")) {
            return url;
        } else {
            return url + '/';
        }
    }

}
