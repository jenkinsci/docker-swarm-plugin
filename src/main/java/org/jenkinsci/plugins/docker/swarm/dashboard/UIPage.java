package org.jenkinsci.plugins.docker.swarm.dashboard;

import hudson.Extension;
import hudson.model.RootAction;

@Extension
public class UIPage implements RootAction {
    @Override
    public String getIconFileName() {
        return "/plugin/docker-swarm/images/24x24/docker.png";
    }

    @Override
    public String getDisplayName() {
        return "Swarm Dashboard";
    }

    @Override
    public String getUrlName() {
        return "swarm-dashboard";
    }




    public Dashboard getDashboard(){
       return new Dashboard();
    }










}

