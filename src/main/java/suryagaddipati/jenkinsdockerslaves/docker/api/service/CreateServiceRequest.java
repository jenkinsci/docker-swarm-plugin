
package suryagaddipati.jenkinsdockerslaves.docker.api.service;

import akka.http.javadsl.model.HttpMethods;
import com.google.common.base.Strings;
import suryagaddipati.jenkinsdockerslaves.docker.api.request.ApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.task.TaskTemplate;
import suryagaddipati.jenkinsdockerslaves.docker.marshalling.ResponseType;

import java.util.ArrayList;
import java.util.List;

public class CreateServiceRequest extends ApiRequest {
    public TaskTemplate TaskTemplate ;
    public String Name;
    public List<String> Networks = new ArrayList<>();
    public CreateServiceRequest(String name, String Image, String[] Cmd, String[] Env) {
        super(HttpMethods.POST, "/services/create",CreateServiceResponse.class, ResponseType.CLASS);
        this.Name = name;
        this.TaskTemplate = new TaskTemplate(Image,Cmd,Env);
    }

    public void  addBindVolume(String source,String target){
        TaskTemplate.ContainerSpec.BindVolume volume = new TaskTemplate.ContainerSpec.BindVolume(source, target);
        this.TaskTemplate.ContainerSpec.Mounts.add(volume);
    }
    public void addCacheVolume(String cacheVolumeName, String target) {
        TaskTemplate.ContainerSpec.CacheDriverVolume volume = new suryagaddipati.jenkinsdockerslaves.docker.api.task.TaskTemplate.ContainerSpec.CacheDriverVolume(cacheVolumeName, target);
        this.TaskTemplate.ContainerSpec.Mounts.add(volume);
    }

    public void setTaskLimits(Long nanoCPUs, Long memoryBytes) {
        this.TaskTemplate.Resources.Limits.NanoCPUs = nanoCPUs;
        this.TaskTemplate.Resources.Limits.MemoryBytes = memoryBytes;
    }

    public void setTaskReservations(Long nanoCPUs, Long memoryBytes) {
        this.TaskTemplate.Resources.Reservations.NanoCPUs = nanoCPUs;
        this.TaskTemplate.Resources.Reservations.MemoryBytes = memoryBytes;
    }

    public void setNetwork(String network) {
        if(!Strings.isNullOrEmpty(network)){
            Networks.add(network);
        }
    }


}
