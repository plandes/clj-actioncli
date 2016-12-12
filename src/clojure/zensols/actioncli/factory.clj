(ns ^{:doc "Conigure and change the log level of the log4j2 system."
      :author "Paul Landes"}
    zensols.actioncli.factory
  (:refer-clojure :exclude [name])
  (:require [clojure.tools.logging.impl :refer :all]))

(defn log4j2-factory
  "Returns a Log4j2-based implementation of the LoggerFactory protocol, or nil if
  not available."
  []
  (try
    (Class/forName "org.apache.logging.log4j.core.Logger")
    (eval
      `(let [levels# {:trace org.apache.logging.log4j.Level/TRACE
                      :debug org.apache.logging.log4j.Level/DEBUG
                      :info  org.apache.logging.log4j.Level/INFO
                      :warn  org.apache.logging.log4j.Level/WARN
                      :error org.apache.logging.log4j.Level/ERROR
                      :fatal org.apache.logging.log4j.Level/FATAL}]
         (extend org.apache.logging.log4j.Logger
           Logger
           {:enabled?
            (fn [^org.apache.logging.log4j.Logger logger# level#]
              (.isEnabled logger#
                 (or
                   (levels# level#)
                   (throw (IllegalArgumentException. (str level#))))))
            :write!
            (fn [^org.apache.logging.log4j.Logger logger# level# e# msg#]
              (let [level# (or
                             (levels# level#)
                             (throw (IllegalArgumentException. (str level#))))]
                (if e#
                  (.log logger# level# msg# e#)
                  (.log logger# level# msg#))))})
         (reify LoggerFactory
           (name [_#]
             "org.apache.logging.log4j")
           (get-logger [_# logger-ns#]
             (org.apache.logging.log4j.LogManager/getLogger (str logger-ns#))))))
    (catch Exception e nil)))
