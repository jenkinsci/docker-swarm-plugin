package suryagaddipati.jenkinsdockerslaves.docker.api;

import akka.http.javadsl.model.StatusCode;

class ApiError {
    public ApiError(Class<?> requestClass, StatusCode statusCode, String message) {
        this.requestClass = requestClass;
        this.statusCode = statusCode;
        this.message = message;
    }

    public Class<?> getRequestClass() {
        return requestClass;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    private Class<?> requestClass;
    private StatusCode statusCode;
    private String message;
}
