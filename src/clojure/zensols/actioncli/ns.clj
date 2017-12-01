(ns ^{:doc "Useful namespace macros.

Some macros taken from
[clojure/clojure-contrib](https://clojure.github.io/clojure-contrib/with-ns-api.html)."}
    zensols.actioncli.ns)

(defmacro with-other-ns
  "Evaluates body in another namespace.  ns is either a namespace
  object or a symbol.  This makes it possible to define functions in
  namespaces other than the current one."
  [ns & body]
  `(binding [*ns* (the-ns ~ns)]
     ~@(map (fn [form] `(eval '~form)) body)))

(defmacro with-temp-ns
  "Evaluates body in an anonymous namespace, which is then immediately
  removed.  The temporary namespace will 'refer' clojure.core."
  [& body]
  `(try
     (create-ns 'sym#)
     (let [result# (with-other-ns 'sym#
                     (clojure.core/refer-clojure)
                     ~@body)]
       result#)
     (finally (remove-ns 'sym#))))

(defmacro with-ns
  "Just like [[with-temp-ns]] but allows an [[clojure.core/ns]]
  specification (not including the namespace name).

  ## Example

  ```
  (with-ns
    [(:require [clojure.string :as s])]
    (s/capitalize \"here\"))
  ```

  See [[clojure.core/ns]]."
  {:style/indent 1}
  [ns-args & body]
  `(try
     (create-ns 'sym#)
     (let [result# (with-other-ns 'sym#
                     (clojure.core/refer-clojure)
                     (ns sym# ~@ns-args)
                     ~@body)]
       result#)
     (finally (remove-ns 'sym#))))

(alter-meta! #'with-ns assoc
             :arglists '([[attr-map? references*] exprs*]))

(defmacro with-context-ns
  "Just like [[with-ns]] but brings in a context into the temporary namespace.

  The **var** parameter is the variable to pass to the temporary namespace.

  ## Example

  ```
  (let [abind 'someval]
    (with-context-ns
        abind
        [(:require [clojure.string :as s])]
      (s/capitalize (str (name abind)))))
  ```

  See [[clojure.core/ns]]."
  {:style/indent 2}
  [var ns-args & body]
  `(try
     (create-ns 'sym#)
     (-> (with-other-ns 'sym#
           (def vns# (atom nil))
           vns#)
         (reset! ~var))
     (let [result# (with-other-ns 'sym#
                     (clojure.core/refer-clojure)
                     (ns sym# ~@ns-args)
                     (let [~var (deref vns#)]
                       ~@body))]
       result#)
     (finally (remove-ns 'sym#))))

(alter-meta! #'with-context-ns assoc
             :arglists '([var [attr-map? references*] exprs*]))
