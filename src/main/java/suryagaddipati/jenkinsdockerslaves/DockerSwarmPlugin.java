package suryagaddipati.jenkinsdockerslaves;

import akka.actor.ActorSystem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.typesafe.config.ConfigFactory;
import hudson.Extension;
import hudson.Plugin;
import jenkins.model.Jenkins;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

@Extension
public class DockerSwarmPlugin extends Plugin {
    private static final Logger LOGGER = Logger.getLogger(DockerSwarmPlugin.class.getName());


    private ActorSystem actorSystem;

    @Override
    public void start() throws Exception {

        File configsDir = new File(Jenkins.getInstance().getRootDir(), "pluginConfigs");
        File swarmConfigYaml = new File(configsDir,"swarm.yml");
        if(swarmConfigYaml.exists()){
            LOGGER.info("Configuring swarm plugin from " + swarmConfigYaml.getAbsolutePath());
            try (InputStream in = new BufferedInputStream(new FileInputStream(swarmConfigYaml))) {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
//                DockerSwarmCloud configuration = mapper.readValue(in, DockerSwarmCloud.class);
//                configuration.save();
            }
        }else {
            LOGGER.info(swarmConfigYaml.getAbsolutePath() + " file not found.");
        }

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
