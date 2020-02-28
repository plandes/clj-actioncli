(ns ^{:doc "Conigure and change the log level of the log4j2 system."
      :author "Paul Landes"}
    zensols.actioncli.log4j2
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:require [clojure.tools.logging.impl :as fac])
  (:import (org.apache.logging.log4j LogManager Level)
           (com.zensols.log LogUtil LogExceptionHandler)))

;; add the Log4J2 factory
(try
  (alter-var-root #'log/*logger-factory*
                  (constantly (fac/log4j2-factory)))
  (catch Exception e))

(def log-level-set-option-default
  "Default log level for [[log-level-set-option]]."
  (Level/toLevel "info"))

(defn- to-level [level-thing]
  (if (string? level-thing)
    (Level/toLevel level-thing)
    level-thing))

(defn change-log-level
  "Change the Log4j2 log level.

  **level-thing** the log level as either the Level instance or a string."
  ([log-name level-thing]
   (log/debugf "new log level: %s -> %s" log-name level-thing)
   (LogUtil/setLevel log-name (to-level level-thing)))
  ([level-thing]
   (log/debugf "new log level: %s" level-thing)
   (LogUtil/setAllLevel (to-level level-thing))))

(defmacro with-log-level
  "Set the the log level to **level-thing** (see [[change-log-level]]) for the
  execution of **body**.

  The **log-name** parameter is one of the following:

  * the string package or namespace name of the logger (usual log name)
  * `:ns` for the current namespace's logger"
  {:style/indent 2}
  [log-name level-thing & body]
  `(let [lname# (if (= :ns ~log-name)
                  (->> (ns-name *ns*) name)
                  ~log-name)
         old# (change-log-level lname# ~level-thing)]
     (try
       ~@body
       (finally
         (change-log-level lname# old#)))))

(defn configure
  "Congigure the Log4j2 system with **res**, which can be anything usable
  with [[clojure.java.io/input-stream]].  If **res** is a string, interpret as
  a resource."
  [res]
  (let [stream (cond (string? res) (io/input-stream (io/resource res))
                     (instance? java.io.InputStream res) res
                     true (io/input-stream res))]
    (if (nil? stream)
      (->> (format "No such resource %s" res)
           (ex-info {:resource res})
           throw))
    (with-open [in stream]
      (LogUtil/config in))
    (log/debugf "configured with XML resource: %s" res)))

(defn log-level-set-option
  "Create an option that sets the Log4j2 level.  Note that all you need to do
  is add this to the option definition and the log level is set in the validate
  phase."
  ([]
   (log-level-set-option "-l" "--level"))
  ([short long]
   [short long "Log level to set in the Log4J2 system."
    :required "<log level>"
    :default log-level-set-option-default
    :parse-fn #(Level/toLevel % nil)
    :validate [(fn [level]
                 (when level
                   (change-log-level level)
                   true))
               "Invalid level (error warn info debug trace)"]]))

(defn forward-uncaught-exceptions
  "Swallow all exceptions by reporting them to the Log4J logging system.

  If **logger** is given, use that Lo4J logger instance for the reporting.  All
  exceptions are handled using the error level."
  ([]
   (LogExceptionHandler/register))
  ([logger]
   (if logger
     (LogExceptionHandler/register logger))))
