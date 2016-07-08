(ns ^{:doc "Dynamic variable and simple cache purging"
      :author "Paul Landes"}
    zensols.actioncli.dynamic)

(def ^:private purge-fns (atom []))

(defn register-purge-fn
  "Register a purge function.  It calls **purge-fn** with 0 arguments.
Multiple calls to this function with the same function is supported.
See [[purge]]."
  [purge-fn]
  (if (= (.indexOf (deref purge-fns) purge-fn) -1)
    (swap! purge-fns conj purge-fn)))

(defn purge
  "Invoke all functions given to [[register-purge-fn]] to clear any cached
  data."
  []
  (doseq [pfn @purge-fns]
    (pfn)))

(defn dyn-init-var
  "Create a variable binding if it isn't already defined.

Usage:
```clojure
(dyn-init-var *ns* 'some-data-var (atom nil))
;(ns-unmap *ns* 'some-data-varl)
```

  The second commented form is useful to *invalidate* a variable so a subsequent
  evaluation of `dyn-init-var` will recreate it.

  * **ns**: the namespace (should be `*ns*`)
  * **var-sym**: the variable name as a symbol to bind
  * **init-val**: the initialization value (usually an atom)"
  ([ns var-sym]
   (dyn-init-var ns var-sym nil))
  ([ns var-sym init-val]
   (binding [*ns* ns]
     (when-not (resolve var-sym)
       (let [new-var (intern *ns* var-sym)]
         (alter-var-root new-var (constantly init-val)))))))
