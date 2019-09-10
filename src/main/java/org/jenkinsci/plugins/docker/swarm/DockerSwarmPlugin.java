package org.jenkinsci.plugins.docker.swarm;

import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;
import hudson.Extension;
import hudson.Plugin;

@Extension
public class DockerSwarmPlugin extends Plugin {

    private ActorSystem actorSystem;

    @Override
    public void start() throws Exception {
        this.actorSystem = ActorSystem.create("swarm-plugin", ConfigFactory.load());

    }

    @Override
    public void stop() throws Exception {
        this.actorSystem.terminate();
    }

    public ActorSystem getActorSystem() {
        return actorSystem;
    }

}
