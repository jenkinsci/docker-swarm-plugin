package suryagaddipati.jenkinsdockerslaves.dashboard;

import hudson.model.Computer;
import hudson.model.Run;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import suryagaddipati.jenkinsdockerslaves.Bytes;
import suryagaddipati.jenkinsdockerslaves.DockerComputer;
import suryagaddipati.jenkinsdockerslaves.docker.api.nodes.Node;
import suryagaddipati.jenkinsdockerslaves.docker.api.task.Task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class SwarmNode {
    private final String healthy;
    private final String name;
    private final long totalCPUs;
    private final long totalMemory;
    private final List<Task> tasks;

    public SwarmNode(final Node node, final List<Task> tasks) {
        this.name = String.format("%s( %s)" ,node.Description.Hostname,node.Spec.Role);
        this.healthy = node.Status.State;
        this.totalCPUs = nanoToCpu(node.Description.Resources.NanoCPUs);
        this.totalMemory = Bytes.toMB( node.Description.Resources.MemoryBytes) ;
        this.tasks = tasks;
    }

    private long nanoToCpu(long nanoCPUs ) {
        return nanoCPUs /1000000000;
    }

    public String getName() {
        return this.name;
    }

    public boolean isHealthy() {
        return this.healthy == "ready";
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


    public Task[] getUnknownRunningTasks(){
        final Jenkins jenkins = Jenkins.getInstance();
       return this.tasks.stream().filter(task -> getComputer(jenkins, task)==null && task.Status.isRunning() ).toArray(Task[]::new);
    }

    public Map<Task,Run> getTaskRunMap(){
        final Jenkins jenkins = Jenkins.getInstance();
        Map<Task,Run> map = new HashMap<>();
        getTasksWithRuns(jenkins).forEach(task -> map.put(task,(Run)getComputer(jenkins,task).getCurrentBuild()) );
        return map;
    }

    private Stream<Task> getTasksWithRuns(Jenkins jenkins) {
        return this.tasks.stream().filter(task -> {
            Computer computer = getComputer(jenkins, task);
            return computer != null && computer instanceof DockerComputer && ((DockerComputer) computer).getCurrentBuild() instanceof  Run;
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

    public float getCPUPercentFull(){
         return 100 - (getTotalCPUs() - getReservedCPUs())/new Float(getTotalCPUs()) * 100;
    }


    public Stream<String> getComputers() {
        return tasks.stream().map(task -> task.Spec.getComputerName());
    }

    public Long getReservedCPUs() {
        return nanoToCpu( tasks.stream().filter(task ->  task.Status.isRunning()).map(task -> task.Spec.Resources.Reservations.NanoCPUs).reduce(0l, Long::sum));
    }
    public Long getReservedMemory() {
        return Bytes.toMB( tasks.stream().filter(task ->  task.Status.isRunning()).map(task -> task.Spec.Resources.Reservations.MemoryBytes).reduce(0l, Long::sum));
    }
    public String getCpuUsageJson(){
        final ArrayList<Object> usage = new ArrayList<>();
        usage.add(Arrays.asList("type", "used"));
        usage.add(Arrays.asList("empty",getTotalCPUs() - getReservedCPUs()));
        usage.add(Arrays.asList("reserved",getReservedCPUs()));
        final JSONArray mJSONArray = new JSONArray();
        mJSONArray.addAll(usage);
        return mJSONArray.toString();
    }
    public String getMemoryUsageJson(){
        final ArrayList<Object> usage = new ArrayList<>();
        usage.add(Arrays.asList("type", "used"));
        usage.add(Arrays.asList("empty",getTotalMemory() - getReservedMemory()));
        usage.add(Arrays.asList("reserved",getReservedMemory()));
        final JSONArray mJSONArray = new JSONArray();
        mJSONArray.addAll(usage);
        return mJSONArray.toString();
    }
}
