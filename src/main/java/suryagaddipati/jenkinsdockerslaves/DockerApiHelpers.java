package suryagaddipati.jenkinsdockerslaves;

import java.util.logging.Level;
import java.util.logging.Logger;

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

    public static void executeSlientlyWithLogging(Runnable runnable, Logger logger,String message) {
        try {
            runnable.run();
        }catch (Exception e){
            logger.log(Level.INFO,message,e);
        }
    }
}
