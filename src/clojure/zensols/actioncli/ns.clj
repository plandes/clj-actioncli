(ns ^{:doc "Useful namespace macros.

Namespace macros taken from
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
  (with-declare-ns
    [(:require [clojure.string :as s])]
    (s/capitalize \"here\"))
  ```"
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
