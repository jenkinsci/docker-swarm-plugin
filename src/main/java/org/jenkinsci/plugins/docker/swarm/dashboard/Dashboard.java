package org.jenkinsci.plugins.docker.swarm.dashboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;

import org.jenkinsci.plugins.docker.swarm.DockerSwarmAgentInfo;
import org.jenkinsci.plugins.docker.swarm.Util;
import org.jenkinsci.plugins.docker.swarm.docker.api.nodes.ListNodesRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.nodes.Node;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiException;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.SerializationException;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ListServicesRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ScheduledService;
import org.jenkinsci.plugins.docker.swarm.docker.api.task.ListTasksRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.task.Task;

import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.Run;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;

public class Dashboard {
    private final List<SwarmNode> nodes;

    public Dashboard() throws IOException {
        this.nodes = calculateNodes();
    }

    public Iterable<SwarmQueueItem> getQueue() {
        final List<SwarmQueueItem> queue = new ArrayList<>();
        final Queue.Item[] items = Jenkins.getInstance().getQueue().getItems();
        for (int i = items.length - 1; i >= 0; i--) { // reverse order
            final Queue.Item item = items[i];
            final DockerSwarmAgentInfo agentInfo = item.getAction(DockerSwarmAgentInfo.class);
            if (agentInfo != null && item instanceof Queue.BuildableItem) {
                queue.add(new SwarmQueueItem((Queue.BuildableItem) item));
            }
        }
        return queue;
    }

    public Boolean isQueueEmpty() {
        return Iterables.isEmpty(getQueue());
    }

    public List<SwarmNode> getNodes() {
        return this.nodes;
    }

    public String getCpuUsage() throws IOException {
        final ArrayList<Object> usage = new ArrayList<>();
        usage.add(Arrays.asList("Job", "cpu"));

        final Map<String, Long> usagePerJob = new HashMap<>();
        final List<SwarmNode> nodes = calculateNodes();
        final long totalAvailable = nodes.stream().map(node -> node.getTotalCPUs()).reduce(0l, Long::sum);
        final long totalReserved = nodes.stream().map(node -> node.getReservedCPUs()).reduce(0l, Long::sum);

        for (final SwarmNode node : nodes) {
            final Map<Task, Run> map = node.getTaskRunMap();
            for (final Task task : map.keySet()) {
                final String jobName = getJobName(map.get(task));
                if (usagePerJob.containsKey(jobName)) {
                    usagePerJob.put(jobName, usagePerJob.get(jobName) + (Long) task.getReservedCpus());
                } else {
                    usagePerJob.put(jobName, task.getReservedCpus());
                }
            }
            for (final Task task : node.getUnknownRunningTasks()) {
                usagePerJob.put(task.getServiceID(), task.getReservedCpus());
            }
        }
        usagePerJob.put("Free ", totalAvailable - totalReserved);

        for (final String jobName : usagePerJob.keySet()) {
            final Long jobUsage = usagePerJob.get(jobName);
            usage.add(Arrays.asList(jobName + " - " + jobUsage, jobUsage));
        }

        final JSONArray mJSONArray = new JSONArray();
        mJSONArray.addAll(usage);
        return mJSONArray.toString();
    }

    public String getMemoryUsage() throws IOException {
        final ArrayList<Object> usage = new ArrayList<>();
        usage.add(Arrays.asList("Job", "memory"));

        final Map<String, Long> usagePerJob = new HashMap<>();
        final List<SwarmNode> nodes = calculateNodes();
        final long totalAvailable = nodes.stream().map(node -> node.getTotalMemory()).reduce(0l, Long::sum);
        final long totalReserved = nodes.stream().map(node -> node.getReservedMemory()).reduce(0l, Long::sum);

        for (final SwarmNode node : nodes) {
            final Map<Task, Run> map = node.getTaskRunMap();
            for (final Task task : map.keySet()) {
                final String jobName = getJobName(map.get(task));
                if (usagePerJob.containsKey(jobName)) {
                    usagePerJob.put(jobName, usagePerJob.get(jobName) + (Long) task.getReservedMemory());
                } else {
                    usagePerJob.put(jobName, task.getReservedMemory());
                }
            }
            for (final Task task : node.getUnknownRunningTasks()) {
                usagePerJob.put(task.getServiceID(), task.getReservedMemory());
            }
        }
        usagePerJob.put("Free ", totalAvailable - totalReserved);

        for (final String jobName : usagePerJob.keySet()) {
            final Long jobUsage = usagePerJob.get(jobName);
            usage.add(Arrays.asList(jobName + " - " + jobUsage, jobUsage));
        }

        final JSONArray mJSONArray = new JSONArray();
        mJSONArray.addAll(usage);
        return mJSONArray.toString();
    }

    private List<SwarmNode> calculateNodes() throws IOException {
        final List<Node> nodeList = getResult(new ListNodesRequest().execute(), List.class);
        final List services = getResult(new ListServicesRequest().execute(), List.class);
        final Object tasks = new ListTasksRequest().execute();
        return toSwarmNodes(services, getResult(tasks, List.class), nodeList);
    }

    private List<SwarmNode> toSwarmNodes(List<ScheduledService> services, List<Task> tasks, List<Node> nodeList) {
        return nodeList.stream().map(node -> {
            Stream<Task> tasksForNode = tasks.stream().filter(task -> task.Status.isRunning())
                    .filter(task -> node.ID.equals(task.NodeID));
            Stream<Task> tasksWithServices = tasksForNode.map(task -> {
                ScheduledService taskService = services.stream()
                        .filter(service -> service.ID.equals(task.getServiceID())).collect(Util.singletonCollector());
                task.service = taskService;
                return task;
            });
            return new SwarmNode(node, tasksWithServices.collect(Collectors.toList()));
        }).sorted(Comparator.comparing(SwarmNode::getName)).collect(Collectors.toList());
    }

    private <T> T getResult(Object result, Class<T> clazz) {
        if (result instanceof SerializationException) {
            throw new RuntimeException(((SerializationException) result).getCause());
        }
        if (result instanceof ApiException) {
            throw new RuntimeException(((ApiException) result).getCause());
        }
        return clazz.cast(result);
    }

    private String getJobName(final Run build) {
        final Job parent = build.getParent();
        return (parent.getParent() instanceof Job ? (Job) parent.getParent() : parent).getFullDisplayName();
    }
}
