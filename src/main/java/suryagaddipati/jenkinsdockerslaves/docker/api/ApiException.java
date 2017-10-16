package suryagaddipati.jenkinsdockerslaves.docker.api;

class ApiException {
    private Class<?> requestClass;

    public Class<?> getRequestClass() {
        return requestClass;
    }

    public Throwable getCause() {
        return cause;
    }

    private Throwable cause;

    public ApiException(Class<?> requestClass,  Throwable cause) {
        this.requestClass = requestClass;
        this.cause = cause;
    }
}
