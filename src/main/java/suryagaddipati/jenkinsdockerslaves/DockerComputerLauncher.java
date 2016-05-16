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
import com.github.dockerjava.api.command.CreateVolumeResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Computer;
import hudson.model.Queue;
import hudson.model.TaskListener;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.SlaveComputer;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerComputerLauncher extends ComputerLauncher {

    private static final Logger LOGGER = Logger.getLogger(DockerComputerLauncher.class.getName());
    private String label;


    private String jobName;


    private AbstractProject job;
    private Queue.BuildableItem bi;


    public DockerComputerLauncher(Queue.BuildableItem bi) {
        this.bi = bi;
        this.job = ((AbstractProject)bi.task);
        this.label = job.getAssignedLabel().getName();
        this.jobName = job.getFullName();
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
            setToInProgress(bi);
            DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
            job.setCustomWorkspace(configuration.getBaseWorkspaceLocation());
            DockerClient dockerClient = configuration.newDockerClient();
            LabelConfiguration labelConfiguration = configuration.getLabelConfiguration(this.label);


            final String additionalSlaveOptions = "-noReconnect";
            final String slaveOptions = "-jnlpUrl " + getSlaveJnlpUrl(computer,configuration) + " -secret " + getSlaveSecret(computer) + " " + additionalSlaveOptions;
            final String[] command = new String[] {"sh", "-c", "curl -o slave.jar " + getSlaveJarUrl(configuration) + " && java -jar slave.jar " + slaveOptions};


            int buildNumber = getBuildNumber();
            String volumeName = getJobName() + "-" + buildNumber;
            CreateVolumeResponse createVolumeResponse = dockerClient.createVolumeCmd().withName(volumeName)
                    .withDriver("cache-driver").exec();
            listener.getLogger().println("Created Volume " + createVolumeResponse.getName() + " at " + createVolumeResponse.getMountpoint());


            CreateContainerCmd containerCmd = dockerClient
                    .createContainerCmd(labelConfiguration.getImage())
                    .withCmd(command)
                    .withPrivileged(configuration.isPrivileged())
                    .withName(computer.getName());

            String[] bindOptions = labelConfiguration.getHostBindsConfig();
            String[] cacheDirs = labelConfiguration.getCacheDirs();
            Bind[]  binds= new Bind[bindOptions.length+cacheDirs.length];
            if(bindOptions.length != 0){
                for(int i = 0; i < bindOptions.length ; i++){
                    String[] bindConfig = bindOptions[i].split(":");
                   binds[i] = new Bind(bindConfig[0], new Volume(bindConfig[1]));
                }
            }

            for(int i = 0; i < cacheDirs.length ; i++){
                listener.getLogger().println("Binding Volume" + cacheDirs[i]+ " to " + createVolumeResponse.getName());
                binds[binds.length-1] = new Bind(createVolumeResponse.getName(),new Volume(cacheDirs[i]));
            }

            containerCmd.withBinds(binds);


            if(labelConfiguration.getCpus() != null){
                containerCmd.withCpuShares( labelConfiguration.getCpus());
            }
            if(labelConfiguration.getMemory() != null){
                containerCmd.withMemory(labelConfiguration.getMemory());
            }

            listener.getLogger().print("Creating Container :" + containerCmd.toString() );
            CreateContainerResponse container = containerCmd.exec();
            listener.getLogger().print("Created container :" + container.getId() );




            bi.getAction(DockerSlaveInfo.class).setContainerId(container);
            dockerClient.startContainerCmd(container.getId()) .exec();
            computer.setContainerId(container.getId());
            computer.setVolumeName(volumeName);
            computer.connect(false).get();
            bi.getAction(DockerSlaveInfo.class).setProvisionedTime(new Date());

        } catch (Exception e) {
            computer.terminate();
            String build = bi + "-" + job.getNextBuildNumber();
            bi.getAction(DockerSlaveInfo.class).setProvisioningInProgress(false);
            if(noResourcesAvailable(e)){
              LOGGER.info("Not resources available for :" + build);
            }else {
                LOGGER.log(Level.INFO,"Failed to schedule: " + build, e);
                bi.getAction(DockerSlaveInfo.class).incrementProvisioningAttemptCount();
            }
            throw new RuntimeException(e);
        }
    }

    private boolean noResourcesAvailable(Exception e) {
        return e instanceof  InternalServerErrorException && e.getMessage().trim().contains("no resources available to schedule container");
    }

    private void setToInProgress(Queue.BuildableItem bi) {
        DockerSlaveInfo slaveInfoAction = bi.getAction(DockerSlaveInfo.class);
        if ( slaveInfoAction != null){
            slaveInfoAction.setProvisioningInProgress(true);
        }else{
            bi.replaceAction(new DockerSlaveInfo(true));
        }
    }

    public int getBuildNumber() {
        List<Queue.Item> queuedItems = getQueuedItems();

        return queuedItems.size()==1 ? job.getNextBuildNumber()
                : job.getNextBuildNumber()+queuedItems.size()-1;
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
    public List<Queue.Item> getQueuedItems() {
        LinkedList<Queue.Item> list = new LinkedList<Queue.Item>();
        for (Queue.Item item : Jenkins.getInstance().getQueue().getApproximateItemsQuickly()) {
            if (item.task == job) {
                list.addFirst(item);
            }
        }
        return list;
    }

    public String getJobName() {
        return jobName.replaceAll("/","_").replaceAll("-","_").replaceAll(",","_").replaceAll(" ","_").replaceAll("\\.","_");
    }
}
