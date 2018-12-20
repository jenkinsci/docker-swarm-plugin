package org.jenkinsci.plugins.docker.swarm;

import hudson.Extension;
import jenkins.slaves.DefaultJnlpSlaveReceiver;
import org.jenkinsci.remoting.engine.JnlpConnectionState;

@Extension(ordinal = 10)
public class DockerSwarmJnlpAgentReceiver extends DefaultJnlpSlaveReceiver {
    @Override
    public void channelClosed( JnlpConnectionState event) {
//        super.channelClosed(event); //be quiet pls
    }
}
