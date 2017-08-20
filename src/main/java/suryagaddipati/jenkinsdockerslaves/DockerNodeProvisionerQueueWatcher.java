package suryagaddipati.jenkinsdockerslaves;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.PeriodicWork;
import hudson.model.Queue;
import jenkins.model.Jenkins;

import java.io.IOException;
import java.util.logging.Logger;

@Extension
public class DockerNodeProvisionerQueueWatcher extends PeriodicWork {
    private static final Logger LOGGER = Logger.getLogger(DockerNodeProvisionerQueueWatcher.class.getName());

    @Override
    public long getRecurrencePeriod() {
        return 10 * 2000;
    }

    @Override
    protected void doRun() throws Exception {
        final Queue.Item[] items = Jenkins.getInstance().getQueue().getItems();
        final DockerSlaveConfiguration slaveConfig = DockerSlaveConfiguration.get();
        for (int i = items.length - 1; i >= 0; i--) { //reverse order
            final Queue.Item item = items[i];
            final DockerSlaveInfo slaveInfo = item.getAction(DockerSlaveInfo.class); // This can be null here if computer was never provisioned. Build will sit in queue forever
            if (slaveInfo != null && item instanceof Queue.BuildableItem) {
                if (slaveInfo.isProvisioningInProgress()) {
                    resetIfStuck(slaveInfo, item);
                } else {
                    processQueueItem(slaveConfig, item, slaveInfo);
                }
            }
        }
    }

    private void resetIfStuck(final DockerSlaveInfo slaveInfo, final Queue.Item item) throws IOException, InterruptedException {
        final DockerLabelAssignmentAction lblAssignmentAction = item.getAction(DockerLabelAssignmentAction.class);
        if (lblAssignmentAction != null) {
            final String computerName = lblAssignmentAction.getLabel().getName();
            final Computer computer = Jenkins.getInstance().getComputer(computerName);
            if (slaveInfo.isComputerProvisioningStuck()) {
                slaveInfo.setProvisioningInProgress(false);
                slaveInfo.incrementProvisioningAttemptCount();
                if (computer != null) {
                    ((DockerComputer) computer).delete();
                }
            }
        }
    }

    private void processQueueItem(final DockerSlaveConfiguration slaveConfig, final Queue.Item item, final DockerSlaveInfo slaveInfo) {
        if (!(slaveInfo.getProvisioningAttempts() > slaveConfig.getMaxProvisioningAttempts())) {
            LOGGER.info("Scheduling build: " + item.task);
            BuildScheduler.scheduleBuild(((Queue.BuildableItem) item));
        } else {
            LOGGER.info("Ignoring " + item.task + " since it exceeded max provisioning attempts. Attempts :" + slaveInfo.getProvisioningAttempts());
        }
    }
}
