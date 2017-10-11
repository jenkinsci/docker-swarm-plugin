package suryagaddipati.jenkinsdockerslaves.docker;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
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
import org.apache.commons.lang.StringUtils;
import scala.PartialFunction;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.util.Failure;
import scala.util.Success;
import suryagaddipati.jenkinsdockerslaves.DockerComputer;

import java.io.PrintStream;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class DockerAgentLauncher extends AbstractActor {
    private DockerComputer computer;
    private PrintStream logger;
    private String dockerUri;
    private CreateServiceRequest createRequest;

    public DockerAgentLauncher(DockerComputer computer, PrintStream logger, String dockerUri) {
        this.computer = computer;
        this.logger = logger;
        this.dockerUri = dockerUri;
    }

    public static Props props(DockerComputer computer, PrintStream logger, String dockerUri) {
        return Props.create(DockerAgentLauncher.class, () -> new DockerAgentLauncher(computer,logger,dockerUri));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateServiceRequest.class, createServiceRequest -> launchContainer(createServiceRequest))
                .build();
    }

    private void launchContainer(CreateServiceRequest createRequest) {
        this.createRequest = createRequest;
        ExecutionContextExecutor ec = getContext().dispatcher();
        Marshaller<Object, RequestEntity> marshaller = Jackson.marshaller();
        Future resEntitry = new Marshal(createRequest).to(marshaller.asScala(), ec);
        DockerAgentLauncher that = this;
        PartialFunction launchContainer = new PartialFunction() {
            @Override
            public boolean isDefinedAt(Object x) {
                return true;
            }
            @Override
            public Object apply(Object result) {
                if(result instanceof Failure){
                    Failure failure = (Failure) result;
                    failure.exception().printStackTrace(logger);
                    return null;
                }else {
                    return ((Success)result).map(that::launchContainer);
                }
            }
        };
        resEntitry.andThen(launchContainer, ec);
    }

    private Object launchContainer(Object createcontainerRequestEnity){
//    String containerName = getSelf().path().name();
        ActorSystem system = getContext().getSystem();
        ActorMaterializer materializer = ActorMaterializer.create(getContext());

        HttpRequest req = HttpRequest.POST(getUrl("/services/create")).withEntity((RequestEntity) createcontainerRequestEnity);
        CompletionStage<HttpResponse> rsp = Http.get(system).singleRequest(req, materializer);
        rsp.whenCompleteAsync(this::serviceStartResponseHandler);
        return createcontainerRequestEnity;
    }

    private void serviceStartResponseHandler(HttpResponse httpResponse, Throwable throwable) {
        ActorMaterializer materializer = ActorMaterializer.create(getContext());
        if(throwable != null){
            throwable.printStackTrace(logger);
            resechedule();
        }else{

            if(httpResponse.status().isFailure()){
                logger.println(httpResponse.entity().toString());
                resechedule();
            }else {
                Unmarshaller<HttpEntity, CreateServiceResponse> unmarshaller = Jackson.unmarshaller(CreateServiceResponse.class);
                unmarshaller.unmarshal(httpResponse.entity(),materializer).thenApply( csr -> {
                    logger.println("Service created with ID : " + csr.ID);
                    if(StringUtils.isNotEmpty(csr.Warning)){
                        logger.println("Service creation warning : " + csr.Warning);
                    }
                    return csr;
                });
            }


        }
    }

    private String getUrl(String path) {
        return this.dockerUri+path;
    }


    private void resechedule() {
        ActorSystem system = getContext().getSystem();
        system.scheduler().scheduleOnce(Duration.apply(12, TimeUnit.SECONDS),getSelf(),createRequest, getContext().dispatcher(), ActorRef.noSender());
    }

}
