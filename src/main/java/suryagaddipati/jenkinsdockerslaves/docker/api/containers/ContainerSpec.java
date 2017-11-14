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
package suryagaddipati.jenkinsdockerslaves.docker.api.containers;

import java.util.ArrayList;
import java.util.List;

public class ContainerSpec {

    public final String Image;
    public final String[] Command;
    public final String[] Env;
    public List<Mount> Mounts = new ArrayList<>();

    public ContainerSpec(){
        this(null,null,null);
    }

    public ContainerSpec(String image, String[] cmd, String[] env) {
        this.Image = image;
        this.Command = cmd;
        this.Env  = env;
    }


    public static  class Mount {
        String Target;
        String Source;
        String Type;
        public Mount.VolumeOptions VolumeOptions;
        public Mount(){
           // for deserilization
        }
        public Mount(String Source, String Target) {
            this.Source = Source;
            this.Target =Target;
        }
        public static Mount bindMount(String Source, String Target){
            Mount mount = new Mount(Source, Target);
            mount.Type ="bind";
            return mount;
        }
        public static Mount cacheMount(String Source, String Target, String cacheDriverName){
            Mount mount = new Mount(Source, Target);
            mount.Type ="volume";
            mount.VolumeOptions = new Mount.VolumeOptions();
            mount.VolumeOptions.DriverConfig = new Mount.VolumeOptions.DriverConfig(cacheDriverName);
            return mount;
        }


        public static class VolumeOptions{
            public Mount.VolumeOptions.DriverConfig DriverConfig ;
            private static class DriverConfig{
                public DriverConfig(){
                    // for deserilization
                }
                public DriverConfig(String name) {
                    Name = name;
                }
                String Name ;
            }
        }

    }
}
