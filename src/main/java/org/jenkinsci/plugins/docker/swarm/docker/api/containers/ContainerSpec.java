package org.jenkinsci.plugins.docker.swarm.docker.api.containers;

import java.util.ArrayList;
import java.util.List;

public class ContainerSpec {

    public final String Image;
    public final String[] Command;
    public final String[] Env;
    public DNSConfig DNSConfig;
    public List<Mount> Mounts = new ArrayList<>();

    public ContainerSpec(){
        this(null,null,null);
    }

    public ContainerSpec(String image, String[] cmd, String[] env) {
        this.Image = image;
        this.Command = cmd;
        this.Env  = env;
        this.DNSConfig = new DNSConfig();
    }

    public static class DNSConfig {
        List<String> Nameservers = new ArrayList<>();
        List<String> Search = new ArrayList<>();
        List<String> Options = new ArrayList<>();
        public DNSConfig() {
            // For deserialization
        }
        public DNSConfig(String nameserver, String search, String option) {
            if (nameserver != null && !nameserver.isEmpty()) {
                this.Nameservers.add(nameserver);
            }
            if (search != null && !search.isEmpty()) {
                this.Search.add(search);
            }
            if (option != null && !option.isEmpty()) {
                this.Options.add(option);
            }
        }
        public void addNameserver(String nameserver) {
            this.Nameservers.add(nameserver);
        }
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

        public static Mount tmpfsMount(String Target) {
            Mount mount = new Mount("", Target);
            mount.Type ="tmpfs";
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
