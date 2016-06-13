package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import jenkins.model.Jenkins;

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
            LOGGER.info("Processing item: "+ item.task);
            DockerSlaveInfo slaveInfo = item.getAction(DockerSlaveInfo.class);
            if( slaveInfo != null && item instanceof Queue.BuildableItem && !slaveInfo.isProvisioningInProgress()){
                if (! (slaveInfo.getProvisioningAttempts() >  slaveConfig.getMaxProvisioningAttempts())){
                    LOGGER.info("Scheduling build: "+ item.task);
                    BuildScheduler.scheduleBuild(((Queue.BuildableItem)item),true);
                }else{
                    LOGGER.info("Ignoring "+ item.task + " since it exceeded max provisioning attempts. Attempts :" + slaveInfo.getProvisioningAttempts());
                }
            }
        }
    }
}
