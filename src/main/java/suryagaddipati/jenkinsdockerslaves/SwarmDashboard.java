package suryagaddipati.jenkinsdockerslaves;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Job;
import hudson.model.Queue;
import hudson.model.RootAction;
import hudson.model.Run;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Extension
public class SwarmDashboard implements RootAction {
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


    public Iterable<SwarmNode> getNodes() {

      return null;
    }

    public String getUsage() {

        final ArrayList<Object> usage = new ArrayList<>();
        usage.add(Arrays.asList("Job", "cpu"));

        final Map<String, Integer> usagePerJob = new HashMap<>();
        int totalCpus = 0;
        int totalReservedCpus = 0;
        for (final SwarmNode node : getNodes()) {
            totalCpus += node.getTotalCPUs();
            for (final Run build : node.getCurrentBuilds()) {
                final String jobName = getJobName(build);
                final Integer reservedCpus = getReservedCPUs(build);
                totalReservedCpus += reservedCpus;
                if (usagePerJob.containsKey(jobName)) {
                    usagePerJob.put(jobName, usagePerJob.get(jobName) + reservedCpus);
                } else {
                    usagePerJob.put(jobName, reservedCpus);
                }
            }
        }
        usagePerJob.put("Available ", totalCpus - totalReservedCpus);

        for (final String jobName : usagePerJob.keySet()) {
            final Integer jobUsage = usagePerJob.get(jobName);
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

    private Integer getReservedCPUs(final Run build) {
        final DockerSlaveInfo slaveInfo = build.getAction(DockerSlaveInfo.class);
        return slaveInfo == null ? 0 : (slaveInfo.getCpuAllocation() == null ? 0 : slaveInfo.getCpuAllocation());
    }

    private List<Computer> filterDockerComputers(final Computer[] computers) {
        final List<Computer> dockerComputers = new ArrayList<>();
        for (int i = 0; i < computers.length; i++) {
            if (computers[i] instanceof DockerComputer)
                dockerComputers.add(computers[i]);
        }
        return dockerComputers;
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
        //Healthy
        private final String healthy;
        private final Iterable<String> computers;
        private final String name;


        private final String reservedCPUs;


        private final String reservedMemory;

        public SwarmNode(final List<Object> info, final List<Computer> dockerComputers) {
            this.name = get(info, 0, 0);
            this.healthy = get(info, 2, 1);
            this.reservedCPUs = get(info, 4, 1);
            this.reservedMemory = get(info, 5, 1);
            final Iterable<Computer> currentComputers = Iterables.filter(dockerComputers, new Predicate<Computer>() {
                public boolean apply(final Computer computer) {
                    final String computerSwarmNodeName = ((DockerComputer) computer).getSwarmNodeName();
                    return computerSwarmNodeName != null && (SwarmNode.this.name.contains(computerSwarmNodeName) || computerSwarmNodeName.contains(SwarmNode.this.name));
                }
            });

            if (!Iterables.isEmpty(currentComputers)) {
                this.computers = Iterables.transform(currentComputers, computer -> computer.getName());
            } else {
                this.computers = new ArrayList<>();
            }
        }

        private static String get(final List<Object> info, final int i, final int j) {
            return (info.get(i) == null || j > ((List<String>) info.get(i)).size() - 1) ? "_/-" : ((List<String>) info.get(i)).get(j);

        }

        public String getName() {
            return this.name;
        }

        public boolean isHealthy() {
            return this.healthy == "Healthy";
        }

        public boolean isFull() {
            final String[] cpus = this.reservedCPUs.split("/");
            return cpus.length == 2 ? cpus[0].trim().equals(cpus[1].trim()) : false;
        }

        public int getTotalCPUs() {
            final String[] cpus = this.reservedCPUs.split("/");
            return cpus.length == 2 ? Integer.parseInt(cpus[1].trim()) : 0;
        }

        public int getComputerCount() {
            return Iterables.size(this.computers);
        }

        public String getReservedCPUs() {
            return this.reservedCPUs;
        }

        public String getReservedMemory() {
            return this.reservedMemory;
        }

        public List<Run> getCurrentBuilds() {
            final Jenkins jenkins = Jenkins.getInstance();
            final List currentBuilds = new ArrayList();
            for (final String computer : this.computers) {
                final Queue.Executable currentBuild = ((DockerComputer) jenkins.getComputer(computer)).getCurrentBuild();
                if (currentBuild instanceof Run) {
                    currentBuilds.add(currentBuild);
                }
            }
            return currentBuilds;
        }

    }


}
