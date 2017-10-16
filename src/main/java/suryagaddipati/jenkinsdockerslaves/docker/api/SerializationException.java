package suryagaddipati.jenkinsdockerslaves.docker.api;

class SerializationException{
    public SerializationException(Throwable cause) {
        this.cause = cause;
    }

    public Throwable cause ;

    public Throwable getCause() {
        return cause;
    }
}
