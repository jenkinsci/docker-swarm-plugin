package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import hudson.model.labels.LabelAssignmentAction;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.logging.Logger;

@Extension
public class DockerNodeProvisionerQueueWatcher extends PeriodicWork {
    private static final Logger LOGGER = Logger.getLogger(DockerNodeProvisionerQueueWatcher.class.getName());
    @Override
    public long getRecurrencePeriod() {
        return 10*2000;
    }

    @Override
    protected void doRun() throws Exception {
        Queue.Item[] items = Jenkins.getInstance().getQueue().getItems();
        DockerSlaveConfiguration slaveConfig = DockerSlaveConfiguration.get();
        for(int i = items.length-1 ; i >=0 ; i-- ){ //reverse order
            Queue.Item item = items[i];
            DockerSlaveInfo slaveInfo = item.getAction(DockerSlaveInfo.class);
            if( slaveInfo != null && item instanceof Queue.BuildableItem ){
                if(slaveInfo.isProvisioningInProgress()){
                    resetIfStuck(slaveInfo,item);
                }else{
                    processQueueItem(slaveConfig, item, slaveInfo);
                }
            }
        }
    }

    private void resetIfStuck(DockerSlaveInfo slaveInfo, Queue.Item item) throws IOException, InterruptedException {
        DockerLabelAssignmentAction lblAssignmentAction = item.getAction(DockerLabelAssignmentAction.class);
        if(lblAssignmentAction != null){
            String computerName = lblAssignmentAction.getLabel().getName();
            Computer computer = Jenkins.getInstance().getComputer(computerName);
                if(slaveInfo.isComputerProvisioningStuck()){
                    slaveInfo.setProvisioningInProgress(false);
                    if(computer != null){
                        new ContainerCleanupListener().terminate((DockerComputer) computer, System.out);
                    }
            }
        }
    }

    private void processQueueItem(DockerSlaveConfiguration slaveConfig, Queue.Item item, DockerSlaveInfo slaveInfo) {
        if (! (slaveInfo.getProvisioningAttempts() >  slaveConfig.getMaxProvisioningAttempts())){
            LOGGER.info("Scheduling build: "+ item.task);
            BuildScheduler.scheduleBuild(((Queue.BuildableItem)item),true);
        }else{
            LOGGER.info("Ignoring "+ item.task + " since it exceeded max provisioning attempts. Attempts :" + slaveInfo.getProvisioningAttempts());
        }
    }
}
