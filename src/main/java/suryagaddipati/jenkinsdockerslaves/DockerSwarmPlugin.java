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

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.http.scaladsl.marshalling.Marshal;
import akka.stream.ActorMaterializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import hudson.Extension;
import hudson.Plugin;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;
import scala.Function1;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.impl.ExecutionContextImpl;
import scala.runtime.BoxedUnit;
import suryagaddipati.jenkinsdockerslaves.docker.CreateContainerRequest;
import suryagaddipati.jenkinsdockerslaves.docker.CreateContainerResponse;
import suryagaddipati.jenkinsdockerslaves.docker.Jackson;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Extension
public class DockerSwarmPlugin extends Plugin {
    private static final Logger LOGGER = Logger.getLogger(DockerSwarmPlugin.class.getName());
    private ActorSystem system;
    private ActorMaterializer materializer;

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

        this.system = ActorSystem.create();
        this.materializer = ActorMaterializer.create(system);
    }

    @Override
    public void stop() throws Exception {
        this.system.shutdown();
    }

    public void launchContainer(String[] command, String name, String[] envVars, LabelConfiguration labelConfiguration, TaskListener listener) {
        CreateContainerRequest crReq = new CreateContainerRequest(labelConfiguration.getImage(), command, envVars);
        Marshaller<CreateContainerRequest, RequestEntity> marshller = Jackson.<CreateContainerRequest>marshaller();
        Unmarshaller<HttpEntity, CreateContainerResponse> unmarshaller = Jackson.unmarshaller(CreateContainerResponse.class);
        Function1<Throwable, BoxedUnit> excp = v1 -> BoxedUnit.UNIT ;
        final ExecutionContext global = ExecutionContextImpl.fromExecutorService(Executors.newFixedThreadPool(10), excp);
        Future resEntitry = new Marshal(crReq).to(marshller.asScala(), global);
        Function1 func = v1 -> {
            System.out.print(v1);
            HttpRequest req = HttpRequest.POST("http://localhost:2376/containers/create").withEntity((RequestEntity) v1);
            CompletionStage<HttpResponse> rsp = Http.get(system).singleRequest(req, materializer);
            rsp.thenApply(v ->  {
                unmarshaller.unmarshal(v.entity(),materializer).thenApply( ccr -> {

                    HttpRequest start = HttpRequest.POST("http://localhost:2376/containers/"+ccr.getId()+"/start");
                    CompletionStage<HttpResponse> startRsp = Http.get(system).singleRequest(start, materializer);
                  return ccr;
                });
                return v;
            });
            return v1;
        };
        resEntitry.map(func,global);
    }
}
