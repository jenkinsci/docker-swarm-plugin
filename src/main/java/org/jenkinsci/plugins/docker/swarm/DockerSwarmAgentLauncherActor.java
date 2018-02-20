package org.jenkinsci.plugins.docker.swarm;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.model.ResponseEntity;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.swarm.docker.api.DockerApiActor;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiError;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiException;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiSuccess;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.SerializationException;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.CreateServiceResponse;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.DeleteServiceRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ServiceLogRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ServiceSpec;
import scala.concurrent.duration.Duration;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerSwarmAgentLauncherActor extends AbstractActor {
    private static final Logger LOGGER = Logger.getLogger(DockerSwarmAgentLauncherActor.class.getName());
    private final ActorRef apiActor;
    private PrintStream logger;
    private ServiceSpec createRequest;

    public DockerSwarmAgentLauncherActor(PrintStream logger ) {
        this.logger = logger;
        this.apiActor = getContext().actorOf(DockerApiActor.props());
    }

    public static Props props(PrintStream logger) {
        return Props.create(DockerSwarmAgentLauncherActor.class, () -> new DockerSwarmAgentLauncherActor(logger));
    }

    @Override
    public Receive createReceive() {
        ReceiveBuilder builder = receiveBuilder();
        return serviceCreateMatchers(builder)

                .match(ApiSuccess.class, apiSuccess -> apiSuccess.getRequestClass().equals(ServiceLogRequest.class), apiSuccess -> serviceLogResponse(apiSuccess.getResponseEntity()) )
                .match(ApiSuccess.class, apiSuccess -> apiSuccess.getRequestClass().equals(DeleteServiceRequest.class), apiSuccess -> serviceDeleted(apiSuccess.getResponseEntity()) )

                .match(DeleteServiceRequest.class, deleteServiceRequest -> deleteService(deleteServiceRequest))

                .match(SerializationException.class, serializationException -> serializationException(serializationException))
                .match(ApiError.class, apiError -> apiError(apiError) )
                .match(ApiException.class, apiException -> apiException.getCause().printStackTrace(logger) )
                .build();
    }

    private void apiError(ApiError apiError) {
        String errorMessage =  apiError.getStatusCode() + " : " + apiError.getMessage();
        logger.println(errorMessage);
        LOGGER.log(Level.SEVERE,errorMessage);
    }

    private void serializationException(SerializationException serializationException) {
        serializationException.getCause().printStackTrace(logger);
        LOGGER.log(Level.SEVERE,"",serializationException.getCause());
    }

    private void serviceDeleted(ResponseEntity responseEntity) {
        getContext().stop(getSelf());
    }

    private void serviceLogResponse(ResponseEntity responseEntity) {
        ActorMaterializer materializer = ActorMaterializer.create(getContext());
        responseEntity.getDataBytes().runForeach(x -> {
            logger.print(x.decodeString(Charset.defaultCharset()));
        } , materializer);
    }

    private  ReceiveBuilder serviceCreateMatchers(ReceiveBuilder builder) {
        return builder.match(ServiceSpec.class, serviceSpec -> createService(serviceSpec))
                .match(CreateServiceResponse.class, createServiceResponse -> createServiceSuccess(createServiceResponse))
                .match(ApiException.class, apiException -> apiException.getRequestClass().equals(ServiceSpec.class), apiException -> serviceCreateException(apiException) );
    }


    private void createService(ServiceSpec createRequest) {
        logger.println(String.format( "[%s] Creating Service with Name : %s" , DateFormat.getTimeInstance().format(new Date()), createRequest.Name));
        this.createRequest = createRequest;
        apiActor.tell(createRequest,getSelf());
    }

    private void createServiceSuccess(CreateServiceResponse createServiceResponse) {
        logger.println(String.format( "[%s] ServiceSpec created with ID : %s" , DateFormat.getTimeInstance().format(new Date()),  createServiceResponse.ID));
        if(StringUtils.isNotEmpty(createServiceResponse.Warning)){
            logger.println("ServiceSpec creation warning : " + createServiceResponse.Warning);
        }
        apiActor.tell(new ServiceLogRequest(createServiceResponse.ID),getSelf());
    }
    private void serviceCreateException(ApiException apiException) {
        apiException.getCause().printStackTrace(this.logger);
        resechedule();
    }

    private void deleteService(DeleteServiceRequest deleteServiceRequest) {
        apiActor.tell(deleteServiceRequest,getSelf());
    }

    private void resechedule() {
        ActorSystem system = getContext().getSystem();
        system.scheduler().scheduleOnce(Duration.apply(12, TimeUnit.SECONDS),getSelf(),createRequest, getContext().dispatcher(), ActorRef.noSender());
    }
}
