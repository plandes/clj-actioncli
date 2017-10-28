(ns ^{:doc "Utility functions"
      :author "Paul Landes"}
    zensols.actioncli.util
  (:import [org.apache.commons.pool2.impl GenericObjectPoolConfig])
  (:require [clojure.tools.logging :as log])
  (:require [clojail.core :as cj])
  (:require [pool.core :as p]))

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

(defmacro defnprime
  "Just like `defn` but call **body** in a [[clojure.core/locking]] lexical
  context to make this first call and resource creation thread-safe.
  Subsequent calls only use the monitor to check if the resource has **body**
  been called once.

  This creates a function **name** that invokes **body** the first time it is
  invoked.  It then caches the solution and returns it on the next call.

  The result it saves is kept in the metadata under key `:init-resource`."
  [name & args]
  (let [{:keys [doc args forms]} (parse-macro-arguments args)]
    `(let [init-inst# (atom false)
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
             (if-let [res2# (deref res#)]
               (do (log/debug "reusing prime result")
                   res2#)
               (do (log/debug "no prime result, calling now")
                   (apply create-fn# ~args))))))
       (alter-meta! (var ~name) assoc :init-resource init-inst#)
       (var ~name))))

(defmacro defnlock
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
         (let [res-name# ~name]
           (log/debugf "fetching resource: %s" res-name#)
           (locking monitor#
             (when-not (deref init-inst#)
               (log/debugf "creating resource: %s" res-name#)
               (swap! init-inst# #(or % (do ~@forms)))
               (log/debug "created resource"))
             (deref init-inst#))))
       (alter-meta! (var ~name) assoc :init-resource init-inst#)
       (var ~name))))

(defmacro defnpool
  "Create a function called **name** that pools objects.  The pool resources
  are bound to **item-symbol** and created with function **factory-fn**.  The
  **config** parameter determines how pooling is done with the following default configuration:

  ```
  {:max-total -1
   :max-idle 8
   :min-idle 0}
  ```

  ## Example
  ```
  (defnpool pool1 [pooled-item
                   {:max-total 5}
                   #(java.util.Date.)]
    [arg1]
    (str pooled-item arg1))
  ```

  Creates a function `pool` that takes one argument (`arg`), binds pooled items
  to `pool-item` with new instances of `java.util.Date` and has a max pool size
  of 5.  The output appends the date to the passed argument.

  See the underlying [Java API](https://commons.apache.org/proper/commons-pool/apidocs/org/apache/commons/pool2/impl/GenericObjectPool.html#setConfig-org.apache.commons.pool2.impl.GenericObjectPoolConfig)
  for more information."
  [name [item-symbol config factory-fn] & args]
  (let [{:keys [doc args forms]} (parse-macro-arguments args)]
    `(let [conf# (merge {:max-total -1
                         :max-idle 8
                         :min-idle 0}
                        ~config)
           pool-inst#
           (doto (p/get-pool ~factory-fn)
             (.setConfig (doto (GenericObjectPoolConfig.)
                           (.setMaxTotal (:max-total conf#))
                           (.setMaxIdle (:max-idle conf#))
                           (.setMinIdle (:min-idle conf#)))))]
       (log/debugf "using config: %s" (pr-str conf#))
       (defn ~(vary-meta name assoc :doc doc) ~args
         (log/debugf "borrow: %s" (trunc pool-inst#))
         (let [~item-symbol (p/borrow pool-inst#)]
           (try
             ~@forms
             (finally
               (log/debugf "return %s" (trunc pool-inst#))
               (p/return pool-inst# ~item-symbol)))))
       (alter-meta! (var ~name) assoc :pool-inst pool-inst#)
       (var ~name))))

(defn pool-item-status
  "Return statistics on the **pool-fn-var**.  A var (not a symbole) needs to be
  passed, which look something like `'#my-pol-fn` for **pool-fn-var**."
  [pool-fn-var]
  (let [pool (->> pool-fn-var meta :pool-inst)]
    ;;.listAllObjects
    {:active (.getNumActive pool)
     :waiters (.getNumWaiters pool)
     :idle (.getNumIdle pool)}))

(doseq [var-fn [#'defnprime #'defnlock]]
  (alter-meta! var-fn assoc
               :arglists '([attr-map? name doc-string? [params*] body]
                           [name doc-string? [params*] body]
                           [name [params*] body])))

(doseq [var-fn [#'defnpool]]
  (alter-meta! var-fn assoc
               :arglists '([attr-map? name [symbol config factory-fn] doc-string? [params*] body]
                           [name [symbol config factory-fn] doc-string? [params*] body]
                           [name [symbol config factory-fn] [params*] body])))
