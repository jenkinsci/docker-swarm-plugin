package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import java.util.List;

@Extension
public class DockerNodeProvisioner extends PeriodicWork {
    @Override
    public long getRecurrencePeriod() {
        return 50000;
    }

    @Override
    protected void doRun() throws Exception {
        List<Queue.Item> items = Jenkins.getInstance().getQueue().getApproximateItemsQuickly();
        for(Queue.Item item : items){
            DockerNodeProvisioningAttempt attempts = item.getAction(DockerNodeProvisioningAttempt.class);
            if( attempts != null && item instanceof Queue.BuildableItem){
              OneshotScheduler.scheduleBuild(((Queue.BuildableItem)item),true);
            }
        }
    }
}
