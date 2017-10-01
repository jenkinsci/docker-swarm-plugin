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
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;

import java.io.PrintStream;
import java.util.concurrent.CompletionStage;

public class DockerAgentLauncher extends AbstractActor {
  private PrintStream logger;

  public DockerAgentLauncher(PrintStream logger) {
    this.logger = logger;
  }

  public static Props props(PrintStream logger) {
    return Props.create(DockerAgentLauncher.class, () -> new DockerAgentLauncher(logger));
  }

  @Override
  public Receive createReceive() {
    return receiveBuilder()
            .match(CreateContainerRequest.class, agentConfig -> launchContainer(agentConfig))
            .build();
  }

  private void launchContainer(CreateContainerRequest createRequest) {
    ExecutionContextExecutor ec = getContext().dispatcher();
    Marshaller<Object, RequestEntity> marshaller = Jackson.marshaller();
    Future resEntitry = new Marshal(createRequest).to(marshaller.asScala(), ec);
    resEntitry.map(this::launchContainer, ec);
  }

  private Object launchContainer(Object createcontainerRequestEnity){
    String containerName = getSelf().path().name();
    ActorSystem system = getContext().getSystem();
    ActorMaterializer materializer = ActorMaterializer.create(getContext());

    HttpRequest req = HttpRequest.POST("http://localhost:2376/containers/create").withEntity((RequestEntity) createcontainerRequestEnity);
    CompletionStage<HttpResponse> rsp = Http.get(system).singleRequest(req, materializer);
    rsp.whenCompleteAsync(this::startContainer);
    return createcontainerRequestEnity;
  }

  private void startContainer(HttpResponse httpResponse, Throwable throwable) {
    ActorSystem system = getContext().getSystem();
    ActorMaterializer materializer = ActorMaterializer.create(getContext());
    if(throwable != null){
        throwable.printStackTrace(logger);
    }else{
      Unmarshaller<HttpEntity, CreateContainerResponse> unmarshaller = Jackson.unmarshaller(CreateContainerResponse.class);
      unmarshaller.unmarshal(httpResponse.entity(),materializer).thenApply( ccr -> {
        HttpRequest start = HttpRequest.POST("http://localhost:2376/containers/"+ccr.getId()+"/start");
        CompletionStage<HttpResponse> startRsp = Http.get(system).singleRequest(start, materializer);
        return ccr;
      });
    }
  }
}
