package com.zensols.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * <p>Swallow all exceptions in this instance of the JVM by reporting them to the
 * logging system.</p>
 *
 * <p>Attribution: shemnon on Stackoverflow.</p>
 *
 * @author shemnon
 * @see <a href="https://stackoverflow.com/questions/75218/how-can-i-detect-when-an-exceptions-been-thrown-globally-in-java"/>
 */
public class LogExceptionHandler implements Thread.UncaughtExceptionHandler {
    private Logger logger;

    private LogExceptionHandler(Logger logger) {
	this.logger = logger;
    }

    public static void register() {
	register(LogManager.getRootLogger());
    }

    public static void register(Logger logger) {
	LogExceptionHandler handler = new LogExceptionHandler(logger);
	Thread.setDefaultUncaughtExceptionHandler(handler);
	System.setProperty("sun.awt.exception.handler",
			   LogExceptionHandler.class.getName());
    }

    public void uncaughtException(Thread t, Throwable e) {
	try {
	    logger.error("unhanded exception caught", e);
	} catch (Throwable t2) {
	    // don't let the exception get thrown out, will cause infinite
	    // looping
	}
    }
}
