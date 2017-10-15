/*
The MIT License (MIT)

Copyright (c) 2016, Groupon, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package suryagaddipati.jenkinsdockerslaves.docker.api.service;

import java.util.ArrayList;
import java.util.List;

public class CreateServiceRequest {
    public TaskTemplate TaskTemplate ;
    public String Name;

    public CreateServiceRequest(String name, String Image, String[] Cmd, String[] Env) {
        this.Name = name;
        this.TaskTemplate = new TaskTemplate(Image,Cmd,Env);
    }

    public void  addBindVolume(String source,String target){
        CreateServiceRequest.TaskTemplate.ContainerSpec.BindVolume volume = new CreateServiceRequest.TaskTemplate.ContainerSpec.BindVolume(source, target);
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
