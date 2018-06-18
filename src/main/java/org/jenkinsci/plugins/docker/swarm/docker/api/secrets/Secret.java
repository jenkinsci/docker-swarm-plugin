package org.jenkinsci.plugins.docker.swarm.docker.api.secrets;

import java.util.Date;

public class Secret {
    public String ID;
    public Date CreatedAt;
    public Date UpdatedAt;
    public SecretSpec Spec;
}
