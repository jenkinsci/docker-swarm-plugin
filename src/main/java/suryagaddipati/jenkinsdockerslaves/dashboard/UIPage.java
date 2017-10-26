package suryagaddipati.jenkinsdockerslaves.dashboard;

import akka.actor.ActorSystem;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.RootAction;
import hudson.model.Run;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import suryagaddipati.jenkinsdockerslaves.Bytes;
import suryagaddipati.jenkinsdockerslaves.DockerComputer;
import suryagaddipati.jenkinsdockerslaves.DockerLabelAssignmentAction;
import suryagaddipati.jenkinsdockerslaves.DockerSlaveConfiguration;
import suryagaddipati.jenkinsdockerslaves.DockerSlaveInfo;
import suryagaddipati.jenkinsdockerslaves.DockerSwarmPlugin;
import suryagaddipati.jenkinsdockerslaves.LabelConfiguration;
import suryagaddipati.jenkinsdockerslaves.docker.api.DockerApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.nodes.ListNodesRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.nodes.Node;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.SerializationException;
import suryagaddipati.jenkinsdockerslaves.docker.api.task.ListTasksRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.task.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Extension
public class UIPage implements RootAction {
    @Override
    public String getIconFileName() {
        return "/plugin/jenkins-docker-slaves/images/24x24/docker.png";
    }

    @Override
    public String getDisplayName() {
        return "Swarm Dashboard";
    }

    @Override
    public String getUrlName() {
        return "swarm-dashboard";
    }


    public Iterable getQueue() {
        final List<SwarmQueueItem> queue = new ArrayList<>();
        final Queue.Item[] items = Jenkins.getInstance().getQueue().getItems();
        for (int i = items.length - 1; i >= 0; i--) { //reverse order
            final Queue.Item item = items[i];
            final DockerSlaveInfo slaveInfo = item.getAction(DockerSlaveInfo.class);
            if (slaveInfo != null && item instanceof Queue.BuildableItem) {
                queue.add(new SwarmQueueItem((Queue.BuildableItem) item));
            }
        }
        return queue;
    }

    public Dashboard getDashboard(){
       return new Dashboard();
    }

    public List<SwarmNode> getNodes() {
        DockerSwarmPlugin swarmPlugin = Jenkins.getInstance().getPlugin(DockerSwarmPlugin.class);
        ActorSystem as = swarmPlugin.getActorSystem();

        CompletionStage<Object> nodesStage = new DockerApiRequest(as, new ListNodesRequest()).execute();
        CompletionStage<Object> swarmNodesFuture = nodesStage.thenComposeAsync(nodes -> {
            if (nodes instanceof List) {
                CompletableFuture<Object> tasksFuture = new DockerApiRequest(as, new ListTasksRequest()).execute().toCompletableFuture();
                return tasksFuture.thenApply(tasks -> {
                    if(tasks instanceof  List){
                        List<Node> nodeList = (List<Node>) nodes;
                        return nodeList.stream().map(node -> {
                            Stream<Task> tasksForNode = ((List<Task>) tasks).stream()
                                    .filter(task -> node.ID.equals(task.NodeID) && task.Spec.getComputerName() != null);
                            return    new SwarmNode(node, tasksForNode.collect(Collectors.toList()));
                        }).collect(Collectors.toList());
                    }
                    return  CompletableFuture.completedFuture(tasks);
                });
            }
            return CompletableFuture.completedFuture(nodes);
        });

        return (List<SwarmNode>) getFuture(swarmNodesFuture);

    }

    private Object getFuture(CompletionStage<Object> future) {
        try {
            Object result = future.toCompletableFuture().get(5, TimeUnit.SECONDS);
            if(result instanceof SerializationException){
                throw new RuntimeException (((SerializationException)result).getCause());
            }
            return result;
        } catch (InterruptedException|ExecutionException|TimeoutException e) {
            throw  new RuntimeException(e);
        }
    }

    public String getUsage() {

        final ArrayList<Object> usage = new ArrayList<>();
        usage.add(Arrays.asList("Job", "cpu"));

        final Map<String, Long> usagePerJob = new HashMap<>();
        List<SwarmNode> nodes = getNodes();
        final long totalCpus = nodes.stream().map(node -> node.getTotalCPUs()).reduce(0l, Long::sum );
        final long totalReservedCpus = nodes.stream().map(node -> node.getReservedCPUs()).reduce(0l, Long::sum);;

        for (final SwarmNode node : getNodes()) {
            Map<Task, Run> map = node.getTaskRunMap();;
            for (final Task task : map.keySet()) {
                final String jobName = getJobName(map.get(task));
                final Long reservedCpus = task.Spec.Resources.Reservations.NanoCPUs/ 1000000000;
                if (usagePerJob.containsKey(jobName)) {
                    usagePerJob.put(jobName, usagePerJob.get(jobName) + reservedCpus);
                } else {
                    usagePerJob.put(jobName, reservedCpus);
                }
            }
        }
        usagePerJob.put("Available ", totalCpus - totalReservedCpus);

        for (final String jobName : usagePerJob.keySet()) {
            final Long jobUsage = usagePerJob.get(jobName);
            usage.add(Arrays.asList(jobName + " - " + jobUsage, jobUsage));
        }


        final JSONArray mJSONArray = new JSONArray();
        mJSONArray.addAll(usage);
        return mJSONArray.toString();
    }

    private String getJobName(final Run build) {
        final Job parent = build.getParent();
        return getTopLevelItem(parent).getFullDisplayName();
    }

    private Job getTopLevelItem(final Job job) {
        return job.getParent() instanceof Job ? (Job) job.getParent() : job;
    }




    public static class SwarmQueueItem {

        private final String name;
        private final String label;
        private final LabelConfiguration labelConfig;
        private final String inQueueSince;
        private final DockerSlaveInfo slaveInfo;
        private Computer provisionedComputer;

        public SwarmQueueItem(final Queue.BuildableItem item) {
            this.name = item.task.getFullDisplayName();
            this.label = item.task.getAssignedLabel().getName();
            this.labelConfig = DockerSlaveConfiguration.get().getLabelConfiguration(this.label);
            this.inQueueSince = item.getInQueueForString();
            this.slaveInfo = item.getAction(DockerSlaveInfo.class); //this should never be null

            final DockerLabelAssignmentAction lblAssignmentAction = item.getAction(DockerLabelAssignmentAction.class);
            if (lblAssignmentAction != null) {
                final String computerName = lblAssignmentAction.getLabel().getName();
                this.provisionedComputer = Jenkins.getInstance().getComputer(computerName);
            }
        }

        public Computer getProvisionedComputer() {
            return this.provisionedComputer;
        }

        public DockerSlaveInfo getSlaveInfo() {
            return this.slaveInfo;
        }

        public String getName() {
            return this.name;
        }

        public String getLabel() {
            return this.label;
        }

        public LabelConfiguration getLabelConfig() {
            return this.labelConfig;
        }

        public String getInQueueSince() {
            return this.inQueueSince;
        }
    }

    public static class SwarmNode {
        private final String healthy;
        private final String name;
        private final long totalCPUs;
        private final long totalMemory;
        private final List<Task> tasks;

        public SwarmNode(final Node node, final List<Task> tasks) {
            this.name = node.Description.Hostname;
            this.healthy = node.Status.State;
            this.totalCPUs = nanoToCpu(node.Description.Resources.NanoCPUs);
            this.totalMemory = Bytes.toMB( node.Description.Resources.MemoryBytes) ;
            this.tasks = tasks;
        }

        private long nanoToCpu(long nanoCPUs ) {
            return nanoCPUs /1000000000;
        }

        private static String get(final List<Object> info, final int i, final int j) {
            return (info.get(i) == null || j > ((List<String>) info.get(i)).size() - 1) ? "_/-" : ((List<String>) info.get(i)).get(j);

        }

        public String getName() {
            return this.name;
        }

        public boolean isHealthy() {
            return this.healthy == "ready";
        }

        public boolean isFull() {
            return false;
        }

        public long getComputerCount() {
            return this.getComputers().count();
        }

        public Object[] getCurrentBuilds() {
            final Jenkins jenkins = Jenkins.getInstance();
            return getTasksWithRuns(jenkins)
                    .map(task ->  getComputer(jenkins, task).getCurrentBuild())
                    .toArray();
        }

        public  Map<Task,Run> getTaskRunMap(){
            final Jenkins jenkins = Jenkins.getInstance();
            Map<Task,Run> map = new HashMap<>();
            getTasksWithRuns(jenkins).forEach(task -> map.put(task,(Run)getComputer(jenkins,task).getCurrentBuild()) );
            return map;
        }

        private Stream<Task> getTasksWithRuns(Jenkins jenkins) {
            return this.tasks.stream().filter(task -> {
                Computer computer = getComputer(jenkins, task);
                return computer instanceof DockerComputer && ((DockerComputer) computer).getCurrentBuild() instanceof  Run;
            } );
        }


        private DockerComputer getComputer(Jenkins jenkins, Task task) {
            return (DockerComputer) jenkins.getComputer("agent-"+task.Spec.getComputerName());
        }

        public long getTotalCPUs() {
            return totalCPUs;
        }

        public long getTotalMemory() {
            return totalMemory;
        }

        public Stream<String> getComputers() {
            return tasks.stream().map(task -> task.Spec.getComputerName());
        }

        public Long getReservedCPUs() {
            return nanoToCpu( tasks.stream().map(task -> task.Spec.Resources.Reservations.NanoCPUs).reduce(0l, Long::sum));
        }
    }
}

