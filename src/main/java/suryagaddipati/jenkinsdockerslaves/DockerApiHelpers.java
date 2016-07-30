package suryagaddipati.jenkinsdockerslaves;

public class DockerApiHelpers {
    public static void retryOnError(Runnable runnable){
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
}
