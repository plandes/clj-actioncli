package com.zensols.log;

import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 * Changing the log level only seems to work from Java (as apposed via
 * Clojure's interop system).
 *
 * @author Paul Landes
 */
public final class LogUtil {
    private void LogUtil() {}

    /** Override the logging level of a given logger */
    public static Level setLevel(String logName, Level level) {
	LoggerContext ctx = (LoggerContext)LogManager.getContext(false);
	Configuration conf = ctx.getConfiguration();
	LoggerConfig lconf = conf.getLoggerConfig(logName);
	Level oldLevel = lconf.getLevel();
	lconf.setLevel(level);
	ctx.updateLoggers(conf);
	return oldLevel;
    }

    /** Reset the level for all loggers, including the root logger. */
    public static void setAllLevel(Level level) {
	LoggerContext ctx = (LoggerContext)LogManager.getContext(false);
	Configuration conf = ctx.getConfiguration();
	LoggerConfig rootConf = conf.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

	rootConf.setLevel(level);

	for (LoggerConfig logConf : conf.getLoggers().values()) {
	    logConf.setLevel(level);
	}

	ctx.updateLoggers(conf);
    }

    /** Configure the system with the XML contents from the input stream. */
    public static void config(InputStream in) throws IOException {
	ConfigurationSource source = new ConfigurationSource(in);
	Configurator.initialize(null, source);
    }
}
