/*
The MIT License (MIT)

Copyright (c) 2016, Groupon, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package suryagaddipati.jenkinsdockerslaves;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
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
    @Override
    public void start() throws Exception {

        File configsDir = new File(Jenkins.getInstance().getRootDir(), "pluginConfigs");
        File swarmConfigYaml = new File(configsDir,"swarm.yml");
        if(swarmConfigYaml.exists()){
            LOGGER.info("Configuring swarm plugin from " + swarmConfigYaml.getAbsolutePath());
            try (InputStream in = new BufferedInputStream(new FileInputStream(swarmConfigYaml))) {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                DockerSlaveConfiguration configuration = mapper.readValue(in, DockerSlaveConfiguration.class);
                configuration.save();
            }
        }else {
            LOGGER.info(swarmConfigYaml.getAbsolutePath() + " file not found.");
        }
    }
}
