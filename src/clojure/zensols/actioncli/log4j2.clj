(ns ^{:doc "Conigure and change the log level of the log4j2 system."
      :author "Paul Landes"}
    zensols.actioncli.log4j2
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io])
  (:require [zensols.actioncli.factory :as fac])
  (:import (org.apache.logging.log4j LogManager Level)
           (com.zensols.log LogUtil)))

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

(defn configure
  "Congigure the Log4j2 system with **res**, which can be anything usable
  with [[clojure.java.io/input-stream]].  If **res** is a string, interpret as
  a resource."
  [res]
  (let [res (if (string? res)
              (io/resource res)
              res)]
    (with-open [in (io/input-stream res)]
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
