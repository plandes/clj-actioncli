(ns ^{:doc "Parse action based command line arguments."
      :author "Paul Landes"}
    zensols.actioncli.parse
  (:require [clojure.string :as s]
            [clojure.tools.cli :refer [parse-opts summarize]])
  (:require [zensols.actioncli.dynamic :refer (defa-)]))

(def ^:dynamic *dump-jvm-on-error*
  "Bind this from the REPL to avoid a system exit when testing the CLI."
  true)

(defa- program-name-inst "prog")

(def ^:private help
  ["-h" "--help" :default false])

(def ^:private global-commands
  [help])

(defn program-name []
  @program-name-inst)

(defn set-program-name [program-name]
  (reset! program-name-inst program-name))

(defn- create-commands [command-context]
  (->> (:command-defs command-context)
       (map (fn [[key pkg-parent pkg-child command-def]]
              (let [req (list 'require `(quote (~pkg-parent ~pkg-child)))]
                (eval req)
                {key (symbol (format "%s.%s/%s" pkg-parent
                                     pkg-child command-def))})))
       (apply merge)
       eval
       (merge (:single-commands command-context))
       (map (fn [[k v]]
              {k (assoc v :name (name k))}))
       (apply merge)))

(defn- execute-command [key opts args]
  (let [command (:app (get (create-commands) key))]
    (apply command opts args)))

(defn handle-exception
  "Handle exceptions thrown from CLI commands."
  [e]
  (if (instance? java.io.FileNotFoundException e)
    (binding [*out* *err*]
      (println (format "%s: io error: %s" (program-name) (.getMessage e))))
    (binding [*out* *err*]
      (println (format "%s: error: %s"
                       (program-name)
                       (if ex-data
                         (.getMessage e)
                         (.toString e))))))
  (if *dump-jvm-on-error*
    (System/exit 1)
    (throw e)))

(defmacro with-exception
  "Wrap a CLI forms in a `try` and call [[handle-exception]] if an exception
  occurs."
  {:style/indent 0}
  [& forms]
  (let [ex (gensym)]
    `(try
       ~@forms
       (catch Exception ~ex
         (handle-exception ~ex)))))

(defn error-msg
  "Print every element of the sequence **errors**.  If there **errors** is a
  singleton then only print one in the common usage format."
  [errors]
  (if (= (count errors) 1)
    (format "%s: %s" (program-name) (first errors))
    (str "The following errors occurred while parsing your command:\n\n"
         (s/join \newline errors))))

(defn- command-help
  ([{:keys [name] :as command}]
   (command-help command (count name)))
  ([{:keys [name description options] :as command} max-len]
   (str (format (str "%-" (+ 4 max-len) "s") name)
        description \newline
        (:summary (parse-opts nil options))
        (if-not (empty? options) \newline))))

(defn- help-msg [command-context commands command-key]
  (let [{:keys [print-help-fn]} command-context
        command-keys (or (:command-print-order command-context)
                         (concat (->> (:command-defs command-context)
                                      (map first))
                                 (keys (:single-commands command-context))))
        name-len (->> (vals commands)
                      (map #(-> % :name name count))
                      (apply max))
        command (get commands command-key)]
    (->> (if command
           (command-help command)
           (->> (map #(get commands %) command-keys)
                (map #(command-help % name-len))
                (s/join \newline)))
         ((or print-help-fn identity)))))

(defn process-arguments
  "Process the command line, which contains the command (action) and arguments
  `args`.

  The command context is a map with actions.  Actions in turn contains what to
  run when an action is given.

  An example of a command context:

```clojure
(defn- create-command-context []
  {:command-defs '((:service com.example service start-server-command)
                   (:repl zensols.actioncli repl repl-command))
   :single-commands {:version version-info-command}
   :default-arguments [\"service\" \"-p\" \"8080\"]})
```

  See the [main document page](https://github.com/plandes/clj-actioncli) for
  more info."
  [command-context & args]
  (let [args (if (empty? args)
               (:default-arguments command-context)
               args)
        {:keys [options arguments summary]} (parse-opts args global-commands
                                                        :in-order true)
        commands (create-commands command-context)
        command-key (keyword (first arguments))
        command (get commands command-key)]
    (if (or (:help options) (nil? command))
      (println (help-msg command-context commands command-key))
      (let [{command-opts :options
             command-args :arguments
             errors :errors}
            (parse-opts arguments (:options command))]
        (if errors
          (do
            (println (error-msg errors))
            (println (command-help command)))
          ((get (get commands command-key) :app)
           command-opts
           (rest command-args)))))))
