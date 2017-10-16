package suryagaddipati.jenkinsdockerslaves.docker.api.response;


import suryagaddipati.jenkinsdockerslaves.docker.api.request.ApiRequest;

public class ApiSuccess {

    private Class<? extends ApiRequest> requestClass;

    public ApiSuccess(Class<? extends ApiRequest> requestClass) {
        this.requestClass = requestClass;
    }

    public Class<? extends ApiRequest> getRequestClass() {
        return requestClass;
    }
}
