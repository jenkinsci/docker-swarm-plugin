package org.jenkinsci.plugins.docker.swarm.docker.api.containers;

import java.util.ArrayList;
import java.util.List;

public class ContainerSpec {

    public final String Image;
    public final String[] Command;
    public final String[] Env;
    public final String Dir;
    public final String User;
    public List<Mount> Mounts = new ArrayList<>();
    public List<Secret> Secrets = new ArrayList<>();
    public List<Config> Configs = new ArrayList<>();

    public ContainerSpec() {
        this(null, null, null, null, null);
    }

    public ContainerSpec(String image, String[] cmd, String[] env, String dir, String user) {
        this.Image = image;
        this.Command = cmd;
        this.Env = env;
        this.Dir = dir;
        this.User = user;
    }

    public static class Secret {
        public FileSpec File;
        String SecretName;
        String SecretID;

        public Secret() {
        }

        public Secret(String secretId, String secretName) {
            this.SecretID = secretId;
            this.SecretName = secretName;
        }

        public static Secret createSecret(String secretId, String secretName, String fileName) {
            Secret secret = new Secret(secretId, secretName);
            secret.File = new Secret.FileSpec();
            secret.File.Name = fileName;
            secret.File.UID = "0";
            secret.File.GID = "0";
            secret.File.Mode = 511;
            return secret;
        }

        public static class FileSpec {
            String Name;
            String UID;
            String GID;
            Integer Mode;
        }
    }

    public static class Config {
        public FileSpec File;
        String ConfigName;
        String ConfigID;

        public Config() {
        }

        public Config(String configId, String configName) {
            this.ConfigID = configId;
            this.ConfigName = configName;
        }

        public static Config createConfig(String configId, String configName, String fileName) {
            Config config = new Config(configId, configName);
            config.File = new Config.FileSpec();
            config.File.Name = fileName;
            config.File.UID = "0";
            config.File.GID = "0";
            config.File.Mode = 511;
            return config;
        }

        public static class FileSpec {
            String Name;
            String UID;
            String GID;
            Integer Mode;
        }
    }

    public static class Mount {
        String Target;
        String Source;
        String Type;
        public Mount.VolumeOptions VolumeOptions;

        public Mount() {
            // for deserilization
        }

        public Mount(String Source, String Target) {
            this.Source = Source;
            this.Target = Target;
        }

        public static Mount bindMount(String Source, String Target) {
            Mount mount = new Mount(Source, Target);
            mount.Type = "bind";
            return mount;
        }

        public static Mount cacheMount(String Source, String Target, String cacheDriverName) {
            Mount mount = new Mount(Source, Target);
            mount.Type = "volume";
            mount.VolumeOptions = new Mount.VolumeOptions();
            mount.VolumeOptions.DriverConfig = new Mount.VolumeOptions.DriverConfig(cacheDriverName);
            return mount;
        }

        public static Mount tmpfsMount(String Target) {
            Mount mount = new Mount("", Target);
            mount.Type = "tmpfs";
            return mount;
        }

        public static class VolumeOptions {
            public Mount.VolumeOptions.DriverConfig DriverConfig;

            private static class DriverConfig {
                public DriverConfig() {
                    // for deserilization
                }

                public DriverConfig(String name) {
                    Name = name;
                }

                String Name;
            }
        }

    }
}
