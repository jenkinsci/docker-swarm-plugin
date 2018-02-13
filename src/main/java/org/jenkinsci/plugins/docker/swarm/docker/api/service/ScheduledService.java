package org.jenkinsci.plugins.docker.swarm.docker.api.service;

import java.util.Date;

public class ScheduledService {
    public String ID;
    public Date CreatedAt;
    public Date UpdatedAt;
    public ServiceSpec Spec;
}
