(ns ^{:doc "Parse action based command line arguments."
      :author "Paul Landes"}
    zensols.actioncli.parse
  (:require [clojure.string :as str])
  (:use [clojure.tools.cli :refer [parse-opts summarize]]))

(def ^:private help
  ["-h" "--help" :flag true :default false])

(def ^:private global-commands
  [help])

(defn- create-commands [command-context]
  (merge
   (eval
    (apply merge
           (map (fn [[key pkg-parent pkg-child command-def]]
                  (let [req (list 'require `(quote (~pkg-parent ~pkg-child)))]
                    (eval req)
                    {key (symbol (format "%s.%s/%s" pkg-parent
                                         pkg-child command-def))}))
                (:command-defs command-context))))
   (:single-commands command-context)))

(defn- execute-command [key opts args]
  (let [command (:app (get (create-commands) key))]
    (apply command opts args)))

(defn- command-help [command commands]
  (str (name command) "\t"
       (:description (command commands)) "\n"
       (:summary (parse-opts nil (:options (command commands)))) "\n\n"))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn process-arguments
  "Process the command line, which contains the command and following "
  [command-context & args-raw]
  (let [args (if (empty? args-raw)
               (:default-arguments command-context)
               args-raw)
        {global-opts :options arguments :arguments summary :summary}
        (parse-opts args global-commands :in-order true)
        commands (create-commands command-context)
        command (keyword (first arguments))]
    (if (or (:help global-opts)
            (nil? (get commands command)))
      (if (get commands command)
        (println (command-help command commands))
        (doall
         (apply print (map #(command-help % commands) (keys commands)))
         (flush)))
      (let [{command-opts :options
             command-args :arguments
             errors :errors}
            (parse-opts arguments (get (get commands command) :options))]
        (if errors
          (do
            (println (error-msg errors))
            (println (command-help command commands)))
          ((get (get commands command) :app) command-opts command-args))))))
