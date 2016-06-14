package suryagaddipati.jenkinsdockerslaves;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Queue;
import hudson.model.RootAction;
import hudson.model.Run;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Extension
public class SwarmDashboard implements RootAction{
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


    public Iterable getQueue(){
        List<SwarmQueueItem> queue = new ArrayList<>();
        Queue.Item[] items = Jenkins.getInstance().getQueue().getItems();
        DockerSlaveConfiguration slaveConfig = DockerSlaveConfiguration.get();
        for(int i = items.length-1 ; i >=0 ; i-- ){ //reverse order
            Queue.Item item = items[i];
            DockerSlaveInfo slaveInfo = item.getAction(DockerSlaveInfo.class);
            if( slaveInfo != null && item instanceof Queue.BuildableItem && !slaveInfo.isProvisioningInProgress()){
                queue.add(new SwarmQueueItem((Queue.BuildableItem)item));
            }
        }
        return  queue;
    }


    public Iterable<SwarmNode> getNodes(){

        DockerSlaveConfiguration configuration = DockerSlaveConfiguration.get();
        try( DockerClient dockerClient = configuration.newDockerClient()) {
            Info info = dockerClient.infoCmd().exec();
            List<Object> nodeInfo = info.getSystemStatus().subList(getNodeIndex(info), info.getSystemStatus().size() - 1);
            List<List<Object>> nodes = Lists.partition(nodeInfo, 10);
            final List<Computer> dockerComputers = filterDockerComputers(Jenkins.getInstance().getComputers());

            return Iterables.transform(nodes, new Function<List<Object>, SwarmNode>() {
                public SwarmNode apply(List<Object> info) {
                    return new SwarmNode(info, dockerComputers);
                }
            });
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private List<Computer> filterDockerComputers(Computer[] computers) {
        List<Computer> dockerComputers = new ArrayList<>();
        for (int i = 0; i < computers.length ; i++) {
            if(computers[i] instanceof  DockerComputer) dockerComputers.add(computers[i]);
        }
        return dockerComputers;
    }

    private int getNodeIndex(Info info) {
        List<Object> systemStatus = info.getSystemStatus();
        for(int i =0; i < systemStatus.size(); i++){
            List<String>  stat = (List<String>) systemStatus.get(i);
            if(stat.get(0).equals("Nodes"))return i+1;
        }
        return 0;
    }


    public static class SwarmQueueItem{

        private final String name;
        private final String label;
        private final LabelConfiguration labelConfig;
        private final String inQueueSince;

        public SwarmQueueItem(Queue.BuildableItem item) {
            name = item.task.getFullDisplayName();
            label = item.task.getAssignedLabel().getName();
            labelConfig = DockerSlaveConfiguration.get().getLabelConfiguration(label);
            inQueueSince = item.getInQueueForString();
        }
        public String getName() {
            return name;
        }

        public String getLabel() {
            return label;
        }

        public LabelConfiguration getLabelConfig() {
            return labelConfig;
        }

        public String getInQueueSince() {
            return inQueueSince;
        }
    }

    public static class SwarmNode{
        //Healthy
        private final String healthy;
        private final Iterable<String> computers;
        private String name;



        private final String reservedCPUs;


        private final String reservedMemory;

        public SwarmNode(List<Object> info, List<Computer> dockerComputers) {
            name = get(info,0,0);
            healthy =  get(info,2,1);
            reservedCPUs = get(info,4,1);
            reservedMemory = get(info,5,1);
            Iterable<Computer> currentComputers = Iterables.filter(dockerComputers, new Predicate<Computer>() {
                public boolean apply(Computer computer) {
                    String computerSwarmNodeName = ((DockerComputer) computer).getSwarmNodeName();
                    computerSwarmNodeName = (computerSwarmNodeName == null ? "" : computerSwarmNodeName.trim());
                    return name.contains(computerSwarmNodeName) || computerSwarmNodeName.contains(name);
                }
            });

            if(!Iterables.isEmpty(currentComputers)){
                this.computers = Iterables.transform(currentComputers, new Function<Computer, String>() {
                    public String apply(Computer computer) {
                        return computer.getName();
                    }
                });
            }else{
                computers = new ArrayList<>();
            }
        }
        private static String get(List<Object> info, int i, int j) {
            return (info.get(i) == null|| ((List<String>)info.get(i)).size()-1 > j)  ? "_/-":  ((List<String>)info.get(i)).get(j);

        }

        public String getName() {
            return name;
        }
        public boolean isHealthy(){
            return healthy == "Healthy" ;
        }
        public boolean isFull(){
            String[] cpus = reservedCPUs.split("/");
            return  cpus.length == 2? cpus[0].trim().equals(cpus[1].trim()): false;
        }

        public int getComputerCount(){
            return Iterables.size(computers) ;
        }
        public String getReservedCPUs() {
            return reservedCPUs;
        }
        public String getReservedMemory() {
            return reservedMemory;
        }

        public List getCurrentBuilds(){
            Jenkins jenkins = Jenkins.getInstance();
            List currentBuilds = new ArrayList();
            for(String computer : computers){
                Queue.Executable currentBuild = ((DockerComputer) jenkins.getComputer(computer)).getCurrentBuild();
                if(currentBuild instanceof Run){
                    currentBuilds.add(currentBuild);
                }
            }
            return  currentBuilds;
        }

    }


}
