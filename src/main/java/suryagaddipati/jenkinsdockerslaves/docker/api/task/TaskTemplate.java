package suryagaddipati.jenkinsdockerslaves.docker.api.task;

import suryagaddipati.jenkinsdockerslaves.docker.api.containers.ContainerSpec;

public class TaskTemplate {
    public suryagaddipati.jenkinsdockerslaves.docker.api.containers.ContainerSpec ContainerSpec ;
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

}
