
package suryagaddipati.jenkinsdockerslaves.docker.api.service;

import akka.actor.ActorSystem;
import akka.http.javadsl.model.HttpMethods;
import com.typesafe.config.ConfigFactory;
import suryagaddipati.jenkinsdockerslaves.docker.api.DockerApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.request.ApiRequest;
import suryagaddipati.jenkinsdockerslaves.docker.api.response.ApiException;
import suryagaddipati.jenkinsdockerslaves.docker.marshalling.Jackson;
import suryagaddipati.jenkinsdockerslaves.docker.marshalling.ResponseType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class ListServicesRequest extends ApiRequest {

    public ListServicesRequest(String url) {
        super(HttpMethods.GET, url, Service.class, ResponseType.LIST);
    }

    public ListServicesRequest(String dockerApiUrl, String url) {
        super(HttpMethods.GET, dockerApiUrl, url, Service.class, ResponseType.LIST);
    }
    public ListServicesRequest() {
        this("/services");
    }
    public  ListServicesRequest(String dockerApiUrl, String filterKey, String filterValue){
       this(dockerApiUrl, "/services?filters="+encodeJsonFilter(filterKey,filterValue));
    }

    private static String encodeJsonFilter(String filterKey, String filterValue) {
        Map<Object,Object> filter = new HashMap<>();
        filter.put(filterKey,new String[]{filterValue});
        try {
            return URLEncoder.encode( Jackson.toJson(filter),"UTF-8");
        } catch (UnsupportedEncodingException e) {
           throw new RuntimeException(e);
        }
    }

    public static void main(String args[]) throws ExecutionException, InterruptedException {

        final ActorSystem as = ActorSystem.create("swarm-plugin", ConfigFactory.load());;

        final CompletionStage<Object> nodesStage = new DockerApiRequest(as, new ListServicesRequest("http://localhost:2376","label","ROLE=jenkins-agent")).execute();
        Object o = nodesStage.toCompletableFuture().get();
        if(o instanceof ApiException){
            ((ApiException)o).getCause().printStackTrace();
        }
        System.out.println(o);
    }

}
