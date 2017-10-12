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
