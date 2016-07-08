(ns ^{:doc "Add a REPL and provide a CLI command."
      :author "Paul Landes"}
    zensols.actioncli.repl
  (:require [clojure.tools.nrepl.server :as replserv])
  (:require [clojure.tools.nrepl.cmdline :as replcmd]))

(def ^:private default-port)

(defn run-server
  "Run REPL server."
  ([]
   (run-server default-port))
  ([opts]
   (let [port (:port opts)
         fmt "nREPL server started on port %d on host 127.0.0.1 - nrepl://127.0.0.1:%d"]
     (replserv/start-server :port port)
     ;; reproduce the leinnigen repl server message so emacs can find the port
     (println (format fmt port port)))))

(def repl-command
  "A CLI command used with the [[zensols.actioncli.parse]] package."
  {:description "start a repl either on the command line or headless with -h"
   :options
   [["-h" "--headless" "start an nREPL server"]
    ["-p" "--port" "the port bind for the repl server"
     :default default-port
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]]
   :app (fn [opts & args]
          (if (:headless opts)
            (run-server opts)
            (replcmd/-main "--interactive")))})
