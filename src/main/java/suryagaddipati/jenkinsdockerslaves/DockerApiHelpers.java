package suryagaddipati.jenkinsdockerslaves;

public class DockerApiHelpers {
    public static void executeWithRetryOnError(Runnable runnable){
       try{
           runnable.run();
       }catch (Exception e){
           try {
               Thread.sleep(5000);
               runnable.run();
           } catch (InterruptedException e1) {
           }
       }
    }

    public static void executeSliently(Runnable runnable) {
        try {
            runnable.run();
        }catch (Exception _){}
    }
}
