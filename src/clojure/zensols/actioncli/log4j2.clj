(ns ^{:doc "Conigure and change the log level of the log4j2 system."
      :author "Paul Landes"}
    zensols.actioncli.log4j2
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:import (org.apache.logging.log4j LogManager Level)
           (org.apache.logging.log4j.core.config Configurator
                                                 ConfigurationSource)))

(defn change-log-level
  "Change the Log4j2 log level.
  *level-thing* is the log level as either the Level instance or a string."
  [level-thing]
  (log/debugf "changing log level to %s" level-thing)
  (let [ctx (LogManager/getContext false)
        config (.getConfiguration ctx)
        log-config (.getLoggerConfig config LogManager/ROOT_LOGGER_NAME)
        loggers (concat (.values (.getLoggers config))
                        [log-config])
        level (if (string? level-thing)
                (Level/toLevel level-thing)
                level-thing)]
    (doseq [logger loggers]
      (.setLevel logger level))
    (.updateLoggers ctx)))

(defn configure [xml-resource]
  (let [resource (io/resource xml-resource)
        stream (.openStream resource)
        source (ConfigurationSource. stream) ]
    (Configurator/initialize nil source)))
