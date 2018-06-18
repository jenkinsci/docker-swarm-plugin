package org.jenkinsci.plugins.docker.swarm.dashboard;

import hudson.Extension;
import hudson.model.RootAction;

import java.io.IOException;

@Extension
public class UIPage implements RootAction {
    @Override
    public String getIconFileName() {
        return "/plugin/docker-swarm-tls/images/24x24/docker.png";
    }

    @Override
    public String getDisplayName() {
        return "Swarm Dashboard";
    }

    @Override
    public String getUrlName() {
        return "swarm-dashboard";
    }




    public Dashboard getDashboard() throws IOException {
       return new Dashboard();
    }










}

