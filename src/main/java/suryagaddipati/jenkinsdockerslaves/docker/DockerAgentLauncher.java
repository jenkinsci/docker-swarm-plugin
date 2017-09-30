package suryagaddipati.jenkinsdockerslaves.docker;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.Http;
import akka.http.javadsl.marshalling.Marshaller;
import akka.http.javadsl.model.HttpEntity;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.RequestEntity;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.http.scaladsl.marshalling.Marshal;
import akka.stream.ActorMaterializer;
import scala.Function1;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.impl.ExecutionContextImpl;
import scala.runtime.BoxedUnit;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;

public class DockerAgentLauncher extends AbstractActor {
  private ActorSystem system;
  private ActorMaterializer materializer;

  public DockerAgentLauncher(ActorSystem system, ActorMaterializer materializer) {
    this.system = system;
    this.materializer = materializer;
  }



  public static Props props(ActorSystem system, ActorMaterializer materializer) {
    return Props.create(DockerAgentLauncher.class, () -> new DockerAgentLauncher(system,materializer));
  }

  static public class AgentConfig {

    public CreateContainerRequest getRequest() {
      return request;
    }

    private CreateContainerRequest request;

    public String getName() {
      return name;
    }

    private String name;

    public AgentConfig(CreateContainerRequest request, String name) {
      this.request = request;
      this.name = name;
    }
  }



  @Override
  public Receive createReceive() {
    return receiveBuilder()
        .match(AgentConfig.class, agentConfig -> launchContainer(agentConfig))
        .build();
  }

  private void launchContainer(AgentConfig agentConfig) {
    Marshaller<CreateContainerRequest, RequestEntity> marshller = Jackson.<CreateContainerRequest>marshaller();
    Unmarshaller<HttpEntity, CreateContainerResponse> unmarshaller = Jackson.unmarshaller(CreateContainerResponse.class);
    Function1<Throwable, BoxedUnit> excp = v1 -> BoxedUnit.UNIT ;
    final ExecutionContext global = ExecutionContextImpl.fromExecutorService(Executors.newFixedThreadPool(10), excp);
    Future resEntitry = new Marshal(agentConfig.getRequest()).to(marshller.asScala(), global);
    Function1 func = v1 -> {
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
