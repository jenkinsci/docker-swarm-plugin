package org.jenkinsci.plugins.docker.swarm;

import org.jenkinsci.remoting.engine.JnlpConnectionState;

import hudson.Extension;
import jenkins.slaves.DefaultJnlpSlaveReceiver;

@Extension(ordinal = 10)
public class DockerSwarmJnlpAgentReceiver extends DefaultJnlpSlaveReceiver {
    @Override
    public void channelClosed(JnlpConnectionState event) {
        // super.channelClosed(event); //be quiet pls
    }
}
