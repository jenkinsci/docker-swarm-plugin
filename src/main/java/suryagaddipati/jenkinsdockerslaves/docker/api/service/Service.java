
package suryagaddipati.jenkinsdockerslaves.docker.api.service;

import akka.http.javadsl.model.HttpMethods;
import com.google.common.base.Strings;
import suryagaddipati.jenkinsdockerslaves.docker.api.containers.ContainerSpec;
import suryagaddipati.jenkinsdockerslaves.docker.api.network.Network;
import suryagaddipati.jenkinsdockerslaves.docker.api.request.ApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.task.TaskTemplate;
import suryagaddipati.jenkinsdockerslaves.docker.marshalling.ResponseType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Service extends ApiRequest {
    public TaskTemplate TaskTemplate ;
    public String Name;
    public Map<String,String> Labels = new HashMap<>();

    public List<Network> Networks = new ArrayList<>();
    public Service(String name, String Image, String[] Cmd, String[] Env) {
        super(HttpMethods.POST, "/services/create",CreateServiceResponse.class, ResponseType.CLASS);
        this.Name = name;
        this.TaskTemplate = new TaskTemplate(Image,Cmd,Env);
    }

    public Service(){
        super(HttpMethods.POST, "", "/services/create",CreateServiceResponse.class, ResponseType.CLASS);
    }

    public void  addBindVolume(String source,String target){
        ContainerSpec.Mount mount = ContainerSpec.Mount.bindMount(source, target);
        this.TaskTemplate.ContainerSpec.Mounts.add(mount);
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
