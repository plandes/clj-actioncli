(ns ^{:doc "Conigure and change the log level of the log4j2 system."
      :author "Paul Landes"}
    zensols.actioncli.log4j2
  (:require [clojure.tools.logging :as log]
            [clojure.tools.logging.impl :as i]
            [clojure.java.io :as io])
  (:import (org.apache.logging.log4j LogManager Level)
           (org.apache.logging.log4j.core.config Configurator
                                                 ConfigurationSource)
           (org.apache.logging.log4j.core.config LoggerConfig)
           (com.zensols.log LogUtil)
           ))

(defn change-log-level
  "Change the Log4j2 log level.

  **level-thing** the log level as either the Level instance or a string."
  [level-thing]
  ;(log/debugf "changing log level to %s" level-thing)
  (let [ctx (LogManager/getContext
             ;(.getClassLoader (.getClass LogManager))
             ;;(.getClassLoader (.getClass *ns*))
             ;(.getClassLoader clojure.lang.RT)
             false)
        config (.getConfiguration ctx)
        log-config (.getLoggerConfig config LogManager/ROOT_LOGGER_NAME
                                     )
        loggers (concat (.values (.getLoggers config))
                        ;[log-config]
                        )
        level (if (string? level-thing)
                (Level/toLevel level-thing)
                level-thing)]
    (println "BEF " log-config "::" (.getName log-config) "::" (.getLevel log-config))
    (.setLevel log-config level)
    (println "AFT " log-config "::" (.getName log-config) "::" (.getLevel log-config))
    ;(.debug log-config "TEST")
    (doseq [logger loggers]
      (println "BEF " logger "::" (.getName logger))
      (org.apache.logging.log4j.LogManager/getLogger (.getName logger))
      (.setLevel logger level)
      (println "AFT " logger "::" (.getName logger))
      (org.apache.logging.log4j.LogManager/getLogger (.getName logger))
      )
    (println ctx)
    ;(Configurator/setAllLevels "zensols.actioncli" (Level/toLevel "trace"))
    (.updateLoggers ctx)
    ))

;;(LogUtil/setLevel "zensols.actioncli.log4j2" )
;; (->> (macroexpand '(log/error "INF"))
;;      clojure.pprint/pprint)

;; (-> (i/get-logger log/*logger-factory* "zensols.actioncli.log4j2")
;;      (.setLevel (Level/toLevel "trace")))

;(enabled? (get-logger (log4j2-factory) "zensols.actioncli") :error)

;(log/error "ERR")
;(log/info "INF")
;(log/debug "DEB--------------------------------------------------")
;(log/trace "TRA--------------------------------------------------")

;(with-open [in (io/input-stream (io/file "resources/log4j2.xml"))] (LogUtil/config in))
;(LogUtil/setAllLevel (Level/toLevel "info"))

;; (change-log-level "trace")

;; (LogUtil/setLevel "zensols" (Level/toLevel "info"))
;; (LogUtil/test)
;(tmp)

;; (org.apache.logging.log4j.LogManager/getLogger "a")
;; (org.apache.logging.log4j.LogManager/getRootLogger)
;; (org.apache.logging.log4j.LogManager/ROOT_LOGGER_NAME)
;; (org.apache.logging.log4j.LogManager/getLogger "zensols.actioncli.log4j2")


;(clojure.tools.logging.impl/find-factory)

;; (.setLevel (org.apache.logging.log4j.LogManager/getLogger "zensols.actioncli")
;;            (Level/toLevel "trace"))


(defn- tmp []
  (with-open [in (io/input-stream (io/file "test-resources/log4j2.xml"))]
    (->> (ConfigurationSource. in)
         (Configurator/initialize nil))))

(defn configure
  "Congigure the Log4j2 system with an XML resource."
  [xml-resource]
  (let [resource (io/resource xml-resource)
        stream (.openStream resource)
        source (ConfigurationSource. stream) ]
    (Configurator/initialize nil source)))

(defn log-level-set-option
  ([]
   (log-level-set-option "-l" "--level"))
  ([short long]
   [short long "Log level to set in the Log4J2 system."
    :required "LOG LEVEL"
    :default (org.apache.logging.log4j.Level/toLevel "info")
    :parse-fn #(org.apache.logging.log4j.Level/toLevel % nil)
    :validate [(fn [level]
                 (when level
                   (change-log-level level)
                   true))
               "Invalid level (error warn info debug trace)"]]))
