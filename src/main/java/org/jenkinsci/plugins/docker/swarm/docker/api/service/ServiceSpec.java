
package org.jenkinsci.plugins.docker.swarm.docker.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;

import org.jenkinsci.plugins.docker.swarm.docker.api.HttpMethod;
import org.jenkinsci.plugins.docker.swarm.docker.api.containers.ContainerSpec;
import org.jenkinsci.plugins.docker.swarm.docker.api.network.EndpointSpec;
import org.jenkinsci.plugins.docker.swarm.docker.api.network.Network;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.task.TaskTemplate;
import org.jenkinsci.plugins.docker.swarm.docker.marshalling.ResponseType;

public class ServiceSpec extends ApiRequest {
    public org.jenkinsci.plugins.docker.swarm.docker.api.task.TaskTemplate TaskTemplate;
    public org.jenkinsci.plugins.docker.swarm.docker.api.network.EndpointSpec EndpointSpec;
    public String Name;
    public Map<String, String> Labels = new HashMap<>();

    public List<Network> Networks = new ArrayList<>();

    public ServiceSpec(String name, String Image, String[] Cmd, String[] Env, String Dir, String User, String[] Hosts)
            throws IOException {
        super(HttpMethod.POST, "/services/create", CreateServiceResponse.class, ResponseType.CLASS);
        this.Name = name;
        this.TaskTemplate = new TaskTemplate(Image, Cmd, Env, Dir, User, Hosts);
        this.EndpointSpec = new EndpointSpec();
    }

    public ServiceSpec() throws IOException {
        super(HttpMethod.POST, "", "/services/create", CreateServiceResponse.class, ResponseType.CLASS, null);
    }

    public void addBindVolume(String source, String target) {
        ContainerSpec.Mount mount = ContainerSpec.Mount.bindMount(source, target);
        this.TaskTemplate.ContainerSpec.Mounts.add(mount);
    }
    
    public void addNamedPipeVolume(String source, String target) {
        ContainerSpec.Mount mount = ContainerSpec.Mount.namedPipeMount(source, target);
        this.TaskTemplate.ContainerSpec.Mounts.add(mount);
    }

    public void addSecret(String secretId, String secretName, String fileName) {
        ContainerSpec.Secret secret = ContainerSpec.Secret.createSecret(secretId, secretName, fileName);
        this.TaskTemplate.ContainerSpec.Secrets.add(secret);
    }

    public void addConfig(String configId, String configName, String fileName) {
        ContainerSpec.Config config = ContainerSpec.Config.createConfig(configId, configName, fileName);
        this.TaskTemplate.ContainerSpec.Configs.add(config);
    }

    public void addDnsIp(String dnsIp) {
        this.TaskTemplate.ContainerSpec.DNSConfig.addNameserver(dnsIp);
    }

    public void addDnsSearchDomain(String dnsSearchDomain) {
        this.TaskTemplate.ContainerSpec.DNSConfig.addSearch(dnsSearchDomain);
    }

    public void addPortBind(String publishedPort, String targetPort, String protocol) {
        this.EndpointSpec.addPortBind(publishedPort, targetPort, protocol);
    }

    public void addCacheVolume(String cacheVolumeName, String target, String cacheDriverName) {
        ContainerSpec.Mount mount = ContainerSpec.Mount.cacheMount(cacheVolumeName, target, cacheDriverName);
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
        if (!Strings.isNullOrEmpty(network)) {
            Networks.add(new Network(network));
        }
    }

    public void addLabel(String key, String value) {
        this.Labels.put(key, value);
    }

    public void setAuthHeader(String username, String password, String email, String serverAddress) {
        Map<String, String> authMap = new HashMap<>();
        if (!username.isEmpty()) {
            authMap.put("username", username);
        }
        if (!password.isEmpty()) {
            authMap.put("password", password);
        }
        if (!email.isEmpty()) {
            authMap.put("email", email);
        }
        if (!serverAddress.isEmpty()) {
            authMap.put("serverAddress", serverAddress);
        }
        String authJson = "";
        try {
            authJson = new ObjectMapper().writeValueAsString(authMap);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (!authJson.isEmpty()) {
            String authBase64 = Base64.getEncoder().encodeToString(authJson.getBytes());
            this.addHeader("X-Registry-Auth", authBase64);
        }
    }
}
