package SMS.utils;

import java.io.IOException;
import java.util.logging.*;

public class LoggerUtil {

    private static final String LOG_FILE = "sms.log";

    public static Logger getLogger(Class<?> clazz) {
        Logger logger = Logger.getLogger(clazz.getName());
        logger.setUseParentHandlers(false); 
        
        if (logger.getHandlers().length == 0) {
            logger.setLevel(Level.ALL);

            try {
                
                FileHandler fileHandler = new FileHandler(LOG_FILE, true);
                fileHandler.setLevel(Level.ALL);
                fileHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(fileHandler);

                
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setLevel(Level.ALL);
                consoleHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(consoleHandler);

            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to create log file handler", e);
            }
        }

        return logger;
    }

    
    public static void logInfo(String msg) {
        getLogger(LoggerUtil.class).info(msg);
    }

    public static void logWarning(String msg) {
        getLogger(LoggerUtil.class).warning(msg);
    }

    public static void logSevere(String msg, Throwable t) {
        getLogger(LoggerUtil.class).log(Level.SEVERE, msg, t);
    }
}
