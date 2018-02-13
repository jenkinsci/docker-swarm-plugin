package suryagaddipati.jenkinsdockerslaves;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExceptionHandlingHelpers {
    public static void executeWithRetryOnError(final Runnable runnable) {
        try {
            runnable.run();
        } catch (final Exception e) {
            try {
                Thread.sleep(5000);
                runnable.run();
            } catch (final Exception e1) {
            }
        }
    }

    public static void executeSliently(final Runnable runnable) {
        try {
            runnable.run();
        } catch (final Exception e) {
        }
    }

    public static void executeSlientlyWithLogging(final Runnable runnable, final Logger logger, final String message) {
        try {
            runnable.run();
        } catch (final Exception e) {
            logger.log(Level.INFO, message, e);
        }
    }

    public static void executeSlientlyWithLogging(final RunnableWithException runnable, final PrintStream s) {
        try {
            runnable.run();
        } catch (final Exception e) {
            e.printStackTrace(s);
        }
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }

    public interface Runnable {
        void run();
    }
}
