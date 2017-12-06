package suryagaddipati.jenkinsdockerslaves.docker.api.task;

import suryagaddipati.jenkinsdockerslaves.docker.api.containers.ContainerSpec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            public long  NanoCPUs;
            public long   MemoryBytes;
        }
    }

    public  static class RestartPolicy {
        public String  Condition = "on-failure";
//        public int  Delay = 10;
        public int  MaxAttempts = 4;
    }

    private  static Pattern computerName = Pattern.compile("(agent-)(\\d+)");

    public String getComputerName(){
        String[] commands = ContainerSpec.Command;
        if(commands!=null && commands.length == 3   ){
            Matcher m = computerName.matcher(commands[2]);
            if(m.find()){
               return m.group(2);
            }
        }
       return null;
    }

}
