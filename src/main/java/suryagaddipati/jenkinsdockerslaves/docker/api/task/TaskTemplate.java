package suryagaddipati.jenkinsdockerslaves.docker.api.task;

import java.util.ArrayList;
import java.util.List;

public class TaskTemplate {
    public ContainerSpec ContainerSpec ;
    public RestartPolicy RestartPolicy= new RestartPolicy();
    public Resources Resources= new Resources();

    public TaskTemplate(){
       //for reading from api
    }

    public TaskTemplate(String image, String[] cmd, String[] env) {
        this.ContainerSpec = new ContainerSpec(image,cmd,env);

    }

    public  static class  Resources {
        public Resources.Resource Limits = new Resources.Resource();
        public Resources.Resource Reservations = new Resources.Resource();

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
        public List<ContainerSpec.Mount> Mounts = new ArrayList<>();

        public ContainerSpec(){
            this(null,null,null);
        }

        public ContainerSpec(String image, String[] cmd, String[] env) {
            this.Image = image;
            this.Command = cmd;
            this.Env  = env;
        }


        public static abstract class Mount {
            String Target;
            String Source;
            String Type = "bind";

            public Mount(String Source, String Target) {
                this.Source = Source;
                this.Target =Target;
            }

        }

        public static class BindVolume extends ContainerSpec.Mount {
            public BindVolume(String Source, String Target) {
                super(Source, Target);
            }
        }

        public static class CacheDriverVolume extends ContainerSpec.Mount {
            ContainerSpec.CacheDriverVolume.VolumeOptions VolumeOptions = new ContainerSpec.CacheDriverVolume.VolumeOptions();

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
