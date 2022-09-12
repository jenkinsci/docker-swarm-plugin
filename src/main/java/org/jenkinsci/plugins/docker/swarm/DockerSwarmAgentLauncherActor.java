package org.jenkinsci.plugins.docker.swarm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.docker.swarm.docker.api.request.ApiRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiError;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiException;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.ApiSuccess;
import org.jenkinsci.plugins.docker.swarm.docker.api.response.SerializationException;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.CreateServiceResponse;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.DeleteServiceRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ServiceLogRequest;
import org.jenkinsci.plugins.docker.swarm.docker.api.service.ServiceSpec;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.japi.pf.ReceiveBuilder;
import okhttp3.Response;
import scala.concurrent.duration.Duration;

public class DockerSwarmAgentLauncherActor extends AbstractActor {
    private static final Logger LOGGER = Logger.getLogger(DockerSwarmAgentLauncherActor.class.getName());
    private PrintStream logger;
    private ServiceSpec createRequest;

    public DockerSwarmAgentLauncherActor(PrintStream logger) {
        this.logger = logger;
    }

    public static Props props(PrintStream logger) {
        return Props.create(DockerSwarmAgentLauncherActor.class, () -> new DockerSwarmAgentLauncherActor(logger));
    }

    @Override
    public Receive createReceive() {
        ReceiveBuilder builder = receiveBuilder();
        return builder.match(DeleteServiceRequest.class, deleteServiceRequest -> deleteService(deleteServiceRequest))
                .match(ServiceSpec.class, serviceSpec -> createService(serviceSpec)).build();
    }

    private void deleteService(DeleteServiceRequest deleteServiceRequest) {
        apiRequestWithErrorHandling(deleteServiceRequest);
        getContext().stop(getSelf());
    }

    private void serviceLogResponse(ApiSuccess apiSuccess) {
        Response response = apiSuccess.getResponse();
        InputStream in = response.body().byteStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {

            String line;
            while ((line = reader.readLine()) != null) {
                logger.println(line);
                logger.flush();
            }
        } catch (IOException unused) {
        } finally {
            response.body().close();
        }
    }

    private void createService(ServiceSpec createRequest) throws IOException {
        logger.println(String.format("[%s] Creating Service with Name : %s",
                DateFormat.getTimeInstance().format(new Date()), createRequest.Name));
        this.createRequest = createRequest;
        handleServiceResponse(createRequest.execute());
    }

    private void handleServiceResponse(Object response) throws IOException {
        if (response instanceof CreateServiceResponse) {
            createServiceSuccess((CreateServiceResponse) response);
        }
        if (response instanceof ApiException) {
            serviceCreateException((ApiException) response);
        }
    }

    private void createServiceSuccess(CreateServiceResponse createServiceResponse) throws IOException {
        logger.println(String.format("[%s] ServiceSpec created with ID : %s",
                DateFormat.getTimeInstance().format(new Date()), createServiceResponse.ID));
        logger.println(String.format("[%s] ServiceSpec request JSON : %s",
                DateFormat.getTimeInstance().format(new Date()), this.createRequest.toJsonString()));
        if (StringUtils.isNotEmpty(createServiceResponse.Warning)) {
            logger.println("ServiceSpec creation warning : " + createServiceResponse.Warning);
        }
        Object result = apiRequestWithErrorHandling(new ServiceLogRequest(createServiceResponse.ID));
        if (result instanceof ApiSuccess) {
            serviceLogResponse((ApiSuccess) result);
        }
    }

    private void serviceCreateException(ApiException apiException) {
        apiException.getCause().printStackTrace(this.logger);
        reschedule();
    }

    private Object apiRequestWithErrorHandling(ApiRequest request) {
        Object result = request.execute();
        if (result instanceof ApiException) {
            logApiException((ApiException) result);
        } else if (result instanceof ApiError) {
            logApiError((ApiError) result);
        } else if (result instanceof SerializationException) {
            logSerializationException((SerializationException) result);
        }
        return result;
    }

    private void logApiError(ApiError apiError) {
        String errorMessage = apiError.getStatusCode() + " : " + apiError.getMessage();
        logger.println(errorMessage);
        LOGGER.log(Level.SEVERE, errorMessage);
    }

    private void logApiException(ApiException apiException) {
        apiException.getCause().printStackTrace(logger);
    }

    private void logSerializationException(SerializationException serializationException) {
        serializationException.getCause().printStackTrace(logger);
        LOGGER.log(Level.SEVERE, "", serializationException.getCause());
    }

    private void reschedule() {
        ActorSystem system = getContext().getSystem();
        system.scheduler().scheduleOnce(Duration.apply(12, TimeUnit.SECONDS), getSelf(), createRequest,
                getContext().dispatcher(), ActorRef.noSender());
    }
}
