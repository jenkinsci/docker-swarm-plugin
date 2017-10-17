
package suryagaddipati.jenkinsdockerslaves.docker.api.service;

import akka.http.javadsl.model.HttpMethods;
import com.google.common.base.Strings;
import suryagaddipati.jenkinsdockerslaves.docker.api.request.ApiRequest;

import java.util.ArrayList;
import java.util.List;

public class CreateServiceRequest extends ApiRequest {
    public TaskTemplate TaskTemplate ;
    public String Name;
    public List<String> Networks = new ArrayList<>();
    public CreateServiceRequest(String name, String Image, String[] Cmd, String[] Env) {
        super(HttpMethods.POST, "/services/create",CreateServiceResponse.class);
        this.Name = name;
        this.TaskTemplate = new TaskTemplate(Image,Cmd,Env);
    }

    public void  addBindVolume(String source,String target){
        CreateServiceRequest.TaskTemplate.ContainerSpec.BindVolume volume = new CreateServiceRequest.TaskTemplate.ContainerSpec.BindVolume(source, target);
        this.TaskTemplate.ContainerSpec.Mounts.add(volume);
    }
    public void addCacheVolume(String cacheVolumeName, String target) {
        CreateServiceRequest.TaskTemplate.ContainerSpec.CacheDriverVolume volume = new CreateServiceRequest.TaskTemplate.ContainerSpec.CacheDriverVolume(cacheVolumeName, target);
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


    public static class TaskTemplate{
        public ContainerSpec ContainerSpec ;
        public RestartPolicy RestartPolicy= new RestartPolicy();
        public Resources Resources= new Resources();

        public TaskTemplate(String image, String[] cmd, String[] env) {
            this.ContainerSpec = new ContainerSpec(image,cmd,env);

        }

        public  static class  Resources {
            public Resource Limits = new Resource();
            public Resource Reservations = new Resource();

            public static class Resource{
                public Long  NanoCPUs;
                public Long   MemoryBytes;
            }
        }

        public  static class RestartPolicy {
            public String  Condition = "none";
        }

        public static class ContainerSpec{

            public final String Image;
            public final String[] Command;
            public final String[] Env;
            public List<Mount> Mounts = new ArrayList<>();

            public ContainerSpec(String image, String[] cmd, String[] env) {
                this.Image = image;
                this.Command = cmd;
                this.Env  = env;
            }


            public static abstract class Mount {
                String Target;
                String Source;

                public Mount(String Source, String Target) {
                    this.Source = Source;
                    this.Target =Target;
                }

            }

            public static class BindVolume extends Mount{
                String Type = "bind";
                public BindVolume(String Source, String Target) {
                    super(Source, Target);
                }
            }

            public static class CacheDriverVolume extends  Mount{
                VolumeOptions VolumeOptions = new VolumeOptions();

                String Type = "volume";
                public CacheDriverVolume(String Source, String Target) {
                    super(Source, Target);
                }
                private static class VolumeOptions{
                    private static class DriverConfig{
                        String Name = "cache-driver";
                    }
                }
            }
        }
    }
}
