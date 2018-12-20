
package org.jenkinsci.plugins.docker.swarm.docker.api.service;

import com.google.common.base.Strings;
import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.containers.ContainerSpec;
import org.jenkinsci.plugins.docker.swarm.docker.api.network.Network;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.task.TaskTemplate;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceSpec extends ApiRequest {
    public org.jenkinsci.plugins.docker.swarm.docker.api.task.TaskTemplate TaskTemplate ;
    public String Name;
    public Map<String,String> Labels = new HashMap<>();

    public List<Network> Networks = new ArrayList<>();
    public ServiceSpec(String name, String Image, String[] Cmd, String[] Env, String Dir, String User) throws IOException {
        super(HttpMethod.POST, "/services/create",CreateServiceResponse.class, ResponseType.CLASS);
        this.Name = name;
        this.TaskTemplate = new TaskTemplate(Image,Cmd,Env,Dir,User);
    }

    public ServiceSpec() throws IOException {
        super(HttpMethod.POST, "", "/services/create",CreateServiceResponse.class, ResponseType.CLASS);
    }

    public void addBindVolume(String source,String target){
        ContainerSpec.Mount mount = ContainerSpec.Mount.bindMount(source, target);
        this.TaskTemplate.ContainerSpec.Mounts.add(mount);
    }

    public void addSecret(String secretId, String secretName, String fileName){
        ContainerSpec.Secret secret = ContainerSpec.Secret.createSecret(secretId, secretName, fileName);
        this.TaskTemplate.ContainerSpec.Secrets.add(secret);
    }
    public void addConfig(String configId, String configName, String fileName){
        ContainerSpec.Config config = ContainerSpec.Config.createConfig(configId, configName, fileName);
        this.TaskTemplate.ContainerSpec.Configs.add(config);
    }
    public void addCacheVolume(String cacheVolumeName, String target, String cacheDriverName) {
        ContainerSpec.Mount mount = ContainerSpec.Mount.cacheMount(cacheVolumeName, target,cacheDriverName);
        this.TaskTemplate.ContainerSpec.Mounts.add(mount);
    }
    public void addTmpfsMount(String tmpfsDir) {
        ContainerSpec.Mount mount = ContainerSpec.Mount.tmpfsMount(tmpfsDir);
        this.TaskTemplate.ContainerSpec.Mounts.add(mount);
    }

    public void setTaskLimits(long nanoCPUs, long memoryBytes) {
        this.TaskTemplate.Resources.Limits.NanoCPUs = nanoCPUs;
        this.TaskTemplate.Resources.Limits.MemoryBytes = memoryBytes;
    }

    public void setTaskReservations(long nanoCPUs, long memoryBytes) {
        this.TaskTemplate.Resources.Reservations.NanoCPUs = nanoCPUs;
        this.TaskTemplate.Resources.Reservations.MemoryBytes = memoryBytes;
    }

    public void setNetwork(String network) {
        if(!Strings.isNullOrEmpty(network)){
            Networks.add(new Network(network));
        }
    }
    public  void addLabel(String key, String value){
        this.Labels.put(key,value);
    }
}
