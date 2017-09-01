(ns ^{:doc "Utility functions"
      :author "Paul Landes"}
    zensols.actioncli.util
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
