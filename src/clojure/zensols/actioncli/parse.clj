(ns ^{:doc "Parse action based command line arguments."
      :author "Paul Landes"}
    zensols.actioncli.parse
  (:require [clojure.string :as s]
            [clojure.tools.logging :as log]
            [clojure.tools.cli :refer [parse-opts] :as cli])
  (:require [zensols.actioncli.dynamic :refer (defa-)]))

(def ^:dynamic *dump-jvm-on-error*
  "Bind this from the REPL to avoid a system exit when testing the CLI."
  true)

(def ^:dynamic *log-error*
  "Log exceptions in [[handle-exception]]."
  false)

(defa- program-name-inst "prog")

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
  (let [msg (.getMessage e)]
    (if *log-error* (log/error e "command line parse error"))
    (if (instance? java.io.FileNotFoundException e)
      (binding [*out* *err*]
        (println (format "%s: io error: %s" (program-name) msg)))
      (binding [*out* *err*]
        (println (format "%s: error: %s"
                         (program-name)
                         (if ex-data
                           (.getMessage e)
                           (.toString e)))))))
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
  ([{:keys [name] :as command} skip-action-name?]
   (command-help command skip-action-name? (count name)))
  ([{:keys [name description options] :as command} skip-action-name? max-len]
   (str (if-not skip-action-name?
          (format (str "%-" (+ 4 max-len) "s") name))
        description \newline
        (:summary (cli/parse-opts nil options))
        (if-not (empty? options) \newline))))

(defn- command-keys [command-context]
  (or (:command-print-order command-context)
      (concat (map first (:command-defs command-context))
              (keys (:single-commands command-context)))))

(defn- help-msg
  ([command-context commands]
   (help-msg command-context commands nil))
  ([command-context commands command-key]
   (let [{:keys [print-help-fn action-mode]} command-context
         command-keys (command-keys command-context)
         name-len (->> (vals commands)
                       (map #(-> % :name name count))
                       (apply max))
         command (get commands command-key)]
     (->> (if command
            (command-help command)
            (->> (map #(get commands %) command-keys)
                 (map #(command-help % (= 'single action-mode) name-len))
                 (s/join \newline)))
          ((or print-help-fn identity))))))

(defn- parse-single [command-context command arguments single-action-mode?
                     & {:keys [print-errors?]
                        :or {print-errors? true}}]
  (let [{app :app option-defs :options} command
        {:keys [options arguments errors summary]}
        (cli/parse-opts arguments option-defs :strict true)]
    (if errors
      (do
        (if print-errors?
          (->> [(error-msg errors) (command-help command single-action-mode?)]
               (map println)
               doall))
        {:errors errors})
      (app options arguments))))

(defn- parse-multi [command-context commands arguments]
  (let [command-keys (command-keys command-context)
        command-key (keyword (first arguments))
        command (get commands command-key)]
    (if (nil? command)
      (println (help-msg command-context commands command-key))
      (parse-single command-context command (rest arguments) false))))

(defn- parse-global [command-context commands arguments]
  (let [{:keys [global-actions]} command-context]
   (->> global-actions
        (map (fn [action]
               (let [res (parse-single command-context action arguments true
                                       :print-errors? false)]
                 (cond (:global-help res)
                       (do
                         (println (help-msg command-context commands))
                         res)
                       (:global-noop res) res))))
        doall)))

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
  (let [arguments (if (empty? args)
                    (:default-arguments command-context)
                    args)
        {:keys [action-mode]} command-context
        single-action-mode? (= action-mode 'single)
        commands (create-commands command-context)
        global-parsed (->> (parse-global command-context commands arguments)
                           (remove nil?))]
    (if-not (empty? global-parsed)
      global-parsed
      (if single-action-mode?
        (parse-single command-context (-> commands vals first) arguments true)
        (parse-multi command-context commands arguments)))))
