package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import java.util.List;
import java.util.logging.Logger;

@Extension
public class DockerNodeProvisionerQueueWatcher extends PeriodicWork {
    private static final Logger LOGGER = Logger.getLogger(DockerNodeProvisionerQueueWatcher.class.getName());
    @Override
    public long getRecurrencePeriod() {
        return 10*1000;
    }

    @Override
    protected void doRun() throws Exception {
        List<Queue.Item> items = Jenkins.getInstance().getQueue().getApproximateItemsQuickly();
        for(Queue.Item item : items){
            DockerSlaveInfo slaveInfo = item.getAction(DockerSlaveInfo.class);
            if( slaveInfo != null && item instanceof Queue.BuildableItem && !slaveInfo.isProvisioningInProgress()){
                LOGGER.info("Scheduling build: "+ item.task);
              OneshotScheduler.scheduleBuild(((Queue.BuildableItem)item),true);
            }
        }
    }
}
