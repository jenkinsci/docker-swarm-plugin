package org.jenkinsci.plugins.docker.swarm.docker.api.task;

public class Status {
     public String State;
     public boolean isRunning(){
         return "running".equals(State);
     }

    public boolean isComplete() {
         return "complete".equals(State);
    }
}
