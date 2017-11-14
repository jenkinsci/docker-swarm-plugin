package suryagaddipati.jenkinsdockerslaves.docker.api.task;

public class Task {
    public TaskTemplate Spec;
    public String NodeID;
    public String ServiceID;
    public String getServiceID(){
        return ServiceID;
    }
    public long getReservedCpus() {
        return Spec.Resources.Reservations.NanoCPUs/ 1000000000;
    }

}
