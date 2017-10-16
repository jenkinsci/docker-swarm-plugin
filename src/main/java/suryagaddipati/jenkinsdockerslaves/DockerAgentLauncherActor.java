package suryagaddipati.jenkinsdockerslaves;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.model.ResponseEntity;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;
import org.apache.commons.lang.StringUtils;
import scala.concurrent.duration.Duration;
import suryagaddipati.jenkinsdockerslaves.docker.api.DockerApiActor;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiError;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiException;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiSuccess;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.SerializationException;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.CreateServiceRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.CreateServiceResponse;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.DeleteServiceRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.service.ServiceLogRequest;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DockerAgentLauncherActor extends AbstractActor {
    private static final Logger LOGGER = Logger.getLogger(DockerAgentLauncherActor.class.getName());
    private final ActorRef apiActor;
    private PrintStream logger;
    private CreateServiceRequest createRequest;

    public DockerAgentLauncherActor(PrintStream logger ) {
        this.logger = logger;
        this.apiActor = getContext().actorOf(DockerApiActor.props());
    }

    public static Props props(PrintStream logger) {
        return Props.create(DockerAgentLauncherActor.class, () -> new DockerAgentLauncherActor(logger));
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
        return builder.match(CreateServiceRequest.class, createServiceRequest -> createService(createServiceRequest))
                .match(CreateServiceResponse.class, createServiceResponse -> createServiceSuccess(createServiceResponse))
                .match(ApiException.class, apiException -> apiException.getRequestClass().equals(CreateServiceRequest.class), apiException -> serviceCreateException(apiException) );
    }


    private void createService(CreateServiceRequest createRequest) {
        this.createRequest = createRequest;
        apiActor.tell(createRequest,getSelf());
    }

    private void createServiceSuccess(CreateServiceResponse createServiceResponse) {
        logger.println("Service created with ID : " + createServiceResponse.ID);
        if(StringUtils.isNotEmpty(createServiceResponse.Warning)){
            logger.println("Service creation warning : " + createServiceResponse.Warning);
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
