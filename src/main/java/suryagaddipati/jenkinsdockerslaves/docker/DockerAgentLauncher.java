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
import akka.util.ByteString;
import org.apache.commons.lang.StringUtils;
import scala.PartialFunction;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.util.Failure;
import scala.util.Success;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerAgentLauncher extends AbstractActor {
    private static final Logger LOGGER = Logger.getLogger(DockerAgentLauncher.class.getName());
    private PrintStream logger;
    private String dockerUri;
    private CreateServiceRequest createRequest;

    public DockerAgentLauncher( PrintStream logger, String dockerUri) {
        this.logger = logger;
        this.dockerUri = dockerUri;
    }

    public static Props props(PrintStream logger, String dockerUri) {
        return Props.create(DockerAgentLauncher.class, () -> new DockerAgentLauncher(logger,dockerUri));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(CreateServiceRequest.class, createServiceRequest -> launchService(createServiceRequest))
                .match(DeleteServiceRequest.class, deleteServiceRequest -> deleteService(deleteServiceRequest))
                .build();
    }

    private void deleteService(DeleteServiceRequest deleteServiceRequest) {
        HttpRequest req = HttpRequest.DELETE(getUrl("/services/"+ deleteServiceRequest.serviceName));
        executeRequest(req,(httpResponse,throwable) -> {
            if(throwable != null){
                LOGGER.log(Level.SEVERE,"",throwable);
            }
            if(httpResponse.status().isFailure()) {
                LOGGER.log(Level.SEVERE, httpResponse.entity().toString());
            }
            getContext().stop(getSelf());
        });
    }

    private void launchService(CreateServiceRequest createRequest) {
        this.createRequest = createRequest;
        marshallAndRun(createRequest,this::launchService);
    }

    private CompletionStage<HttpResponse> launchService(Object createcontainerRequestEnity){
        HttpRequest req = HttpRequest.POST(getUrl("/services/create")).withEntity((RequestEntity) createcontainerRequestEnity);
        return executeRequest(req,this::serviceStartResponseHandler);
    }

    final Function<ByteString, ByteString> transformEachLine = line -> line /* some transformation here */;
    final int maximumFrameLength = 256;

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


                    HttpRequest logsRequest = HttpRequest.GET(getUrl("/services/" + csr.ID + "/logs?follow=true&stdout=true&stderr=true"));
                    executeRequest(logsRequest, (response,err)->{
                        response.entity().getDataBytes().runForeach(x -> {
                           logger.print(x.decodeString(Charset.defaultCharset()));
                        } , materializer);
                    });
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

    private  void marshallAndRun(Object object, scala.Function1 andThen){
        ExecutionContextExecutor ec = getContext().dispatcher();
        Marshaller<Object, RequestEntity> marshaller = Jackson.marshaller();
        Future entity = new Marshal(object).to(marshaller.asScala(), ec);
        PartialFunction nextAction = new PartialFunction() {
            @Override
            public boolean isDefinedAt(Object x) {
                return true;
            }
            @Override
            public Object apply(Object result) {
                if(result instanceof Failure){ // This would be caused if there is a bug in code, shouldn't be rescheduled here.
                    Failure failure = (Failure) result;
                    failure.exception().printStackTrace(logger);
                    return null;
                }else {
                    return ((Success)result).map(andThen);
                }
            }
        };

        entity.andThen(nextAction,ec);
    }
    private CompletionStage<HttpResponse> executeRequest(HttpRequest req, BiConsumer<HttpResponse, ? super Throwable> onComplete){
        ActorSystem system = getContext().getSystem();
        ActorMaterializer materializer = ActorMaterializer.create(getContext());
        CompletionStage<HttpResponse> rsp = Http.get(system).singleRequest(req, materializer);
        return rsp.whenCompleteAsync(onComplete);

    }
}
