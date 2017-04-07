(ns ^{:doc "Add a REPL and provide a CLI command."
      :author "Paul Landes"}
    zensols.actioncli.repl
  (:require [clojure.tools.nrepl.server :as replserv])
  (:require [clojure.tools.nrepl.cmdline :as replcmd]))

(def ^:private default-port 12345)

(defn run-server
  "Run REPL server."
  ([]
   (run-server default-port))
  ([port]
   (let [fmt "nREPL server started on port %d on host 127.0.0.1 - nrepl://127.0.0.1:%d"]
     (replserv/start-server :port port)
     ;; reproduce the leinnigen repl server message so emacs can find the port
     (println (format fmt port port)))))

(defn repl-port-set-option
  ([]
   (repl-port-set-option "-p" "--port"))
  ([short long]
   (repl-port-set-option short long default-port))
  ([short long port]
   [short long "the port bind for the repl server"
    :required "<number>"
    :default port
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]))

(def repl-command
  "A CLI command used with the [[zensols.actioncli.parse]] package."
  {:description "start a repl either on the command line or headless with -h"
   :options
   [["-h" "--headless" "start an nREPL server"]
    (repl-port-set-option)]
   :app (fn [{:keys [port headless]} & args]
          (if headless
            (run-server port)
            (replcmd/-main "--interactive")))})
