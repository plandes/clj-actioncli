(ns ^{:doc "Utility functions"
      :author "Paul Landes"}
    zensols.actioncli.util
  (:require [clojure.tools.logging :as log])
  (:require [clojail.core :as cj]))

;; duplicated from zensols.tools.string since its bloated with excel deps
(def ^:dynamic *trunc-len*
  "Default truncation length for [[trunc]]."
  80)

(defn trunc
  "Truncate string `obj` at `len` characters adding ellipses if larger that a set
  length.  If `obj` isn't a string use `pr-str` to make it a string.

  See [[*trunc-len*]]."
  ([obj] (trunc obj *trunc-len*))
  ([obj len]
   (let [s (if (string? obj) obj (pr-str obj))
         slen (count s)
         trunc? (> slen len)
         maxlen (-> (if trunc? (min slen (- len 3))
                        (min slen len))
                    (max 0))]
     (str (subs s 0 maxlen) (if trunc? "...")))))

(defmacro with-timeout
  "Execute **body** and timeout after **timeout-millis** milliseconds.

If the execution times out `java.util.concurrent.TimeoutException` is thrown."
  [timeout-millis & body]
  {:style/indent 1}
  `(cj/thunk-timeout (fn [] ~@body) ~timeout-millis))

(defmacro prime-resource-factory
  "Create a function and name space resources that create a shared per/thread."
  [name create-fn]
  `(do
     (def ^:private monitor# (Object.))
     (def ^:private init-inst# (atom false))
     (def ^:private ~name
       {:init-inst init-inst#
        :create-fn (fn [& create-args#]
                     (let [res# (volatile! nil)
                           res-name# ~name]
                       (log/debugf "access resource: %s" res-name#)
                       (locking monitor#
                         (when-not (deref init-inst#)
                           (log/debugf "start priming: %s" res-name#)
                           (vreset! res# (apply ~create-fn create-args#))
                           (reset! init-inst# true)
                           (log/debug "end prime")))
                       (if (deref res#)
                         (do (log/debug "reusing prime result")
                             (deref res#))
                         (do (log/debug "no prime result, calling now")
                             (apply ~create-fn create-args#)))))})))


(defn parse-macro-arguments [arg-list]
  (let [args (java.util.LinkedList. arg-list)
        mres (transient {})]
    (if (and (.peek args)
             (= (type (.peek args)) java.lang.String))
      (assoc! mres :doc (.pop args)))
    (if (empty? args)
      (-> (format "Missing [params*] argument list: %s" arg-list)
          (ex-info  {:args arg-list})
          throw))
    (assoc! mres :args (.pop args))
    (assoc! mres :forms (into [] args))
    (persistent! mres)))

(defmacro def-prime
  "Just like `defn` but call **body** in a [[clojure.core/locking]] lexical
  context to make this first call and resource creation thread-safe.
  Subsequent calls only use the monitor to check if the resource has **body**
  been called once.

  This creates a function **name** that invokes **body** the first time it is
  invoked.  It then caches the solution and returns it on the next call.

  The result it saves is kept in the metadata under key `:init-resource`."
  [name & args]
  (let [{:keys [doc args forms]} (parse-macro-arguments args)]
    `(let [init-inst# (atom nil)
           monitor# (Object.)]
       (defn ~(vary-meta name assoc :doc doc) ~args
         (letfn [(create-fn# ~args
                   (do ~@forms))]
           (let [res# (volatile! nil)]
             (log/debugf "access resource: %s" ~name)
             (locking monitor#
               (when-not (deref init-inst#)
                 (log/debugf "start priming: %s" ~name)
                 (vreset! res# (apply create-fn# ~args))
                 (reset! init-inst# true)
                 (log/debug "end prime")))
             (if (deref res#)
               (do (log/debug "reusing prime result")
                   (deref res#))
               (do (log/debug "no prime result, calling now")
                   (apply create-fn# ~args))))))
       (alter-meta! (var ~name) assoc :init-resource init-inst#)
       (var ~name))))

(defmacro def-lockres
  "Just like `defn` but create an atom instance used to generate the result
  (resource) by calling **body** only once.

  This creates a function **name** that invokes **body** the first time it is
  invoked.  It then caches the solution and returns it on the next call.  This
  is all done in a [[clojure.core/locking]] lexical context to make this first
  call and resource creation thread-safe.

  The result it saves is kept in the metadata under key `:init-resource`."
  [name & args]
  (let [{:keys [doc args forms]} (parse-macro-arguments args)]
    `(let [init-inst# (atom nil)
           monitor# (Object.)]
       (defn ~(vary-meta name assoc :doc doc) ~args
         (let [res# (volatile! nil)
               res-name# ~name]
           (log/debugf "fetching resource: %s" res-name#)
           (locking init-inst#
             (when-not (deref init-inst#)
               (log/debugf "creating resource: %s" res-name#)
               (swap! init-inst#
                      #(or % (do ~@forms)))
               (log/debug "create resource")))
           (deref init-inst#)))
       (alter-meta! (var ~name) assoc :init-resource init-inst#)
       (var ~name))))

(doseq [var-fn [#'def-prime #'def-lockres]]
  (alter-meta! var-fn assoc
               :arglists '([attr-map? name doc-string? [params*] body]
                           [name doc-string? [params*] body]
                           [name [params*] body])))
