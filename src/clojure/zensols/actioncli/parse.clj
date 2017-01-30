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

(defn program-name
  "Return the program name used for info/error message.  This is set
  with [[set-program-name]]."
  []
  @program-name-inst)

(defn set-program-name
  "Set the program name used for info/error message."
  [program-name]
  (reset! program-name-inst program-name))

(defn help-option
  "Return an option that provides version information."
  ([]
   (help-option "-h" "--help"))
  ([short long]
   {:description "print help information and exit"
    :options [[short long]]
    :app (fn [{:keys [help]} & args]
           (if help
             {:global-help true
              :global-noop true}))}))

(defn version-option
  "Return an option that provides version information.  The option uses
  **print-fn** to print the version of the program."
  ([print-fn]
   (version-option print-fn "-v" "--version"))
  ([print-fn short long]
   {:description "print version and exit"
    :options [[short long]]
    :app (fn [{:keys [version]} & args]
           (when version
             (print-fn)
             {:global-noop true}))}))

(defn create-default-usage-format
  "Create a function that formats the usage statement."
  ([]
   (create-default-usage-format "usage: %s%s [options]"))
  ([usage-format]
   (fn [action-names]
     (->> (format usage-format
                  (program-name)
                  (if-not action-names
                    ""
                    (->> action-names
                         (s/join "|")
                         (format " <%s>"))))))))

(defn multi-action-context
  "Create a multi-action context with map **action**.  These don't include the
  name of the action on the command line and are typical UNIX like command
  lines (ex: `ls`, `grep`, etc).

  The parameter is **action** is a list of list of symbols takes the form:
```
'((:action-name1 package1 action1)
  ...)
```
  where **action-name** is a key, **package** is the package and **action** is
  the action definition.

Keys
----
* **help-option:** the help action command, which defaults to [[help-option]]
* **version-option:** usually created with [[version-option]]
* **global-actions:** addition global actions in addition to the help and
  version options listed above
* **action-print-order:** an optional sequence of action name keys indicating
  what order to print action help
* **print-help-fn:** an optional function that takes a string argument of the
  generated option help to provide a way to customize the help message
* **usage-format-fn:** generate the usage portion of the help message that
  takes the names of all action if generating multi-action usage or nil for
  single action usage messages"
  [actions &
   {:keys [global-actions help-option version-option
           action-print-order print-help-fn usage-format-fn]
    :or {help-option (help-option)}}]
  (merge (if action-print-order {:action-print-order action-print-order})
         (if print-help-fn {:print-help-fn print-help-fn})
         {:usage-format-fn (or usage-format-fn (create-default-usage-format))}
         {:action-definitions actions
          :global-actions (concat global-actions
                                  (if help-option [help-option])
                                  (if version-option [version-option]))
          :action-mode 'multi}))

(defn single-action-context
  "Create a single action context with map **action**.  These don't include the
  name of the action on the command line and are typical UNIX like command
  lines (ex: `ls`, `grep`, etc).

  The parameter is **action** is a list of symbols that takes the form:
```
'(package action)
```
  where **package** is the package and **action** is the action definition.

  See [[multi-action-context]] for the description of **options**."
  [action & options]
  (-> (cons :single-action action)
      list
      (#(apply multi-action-context % options))
      (merge {:action-mode 'single})))

(defn- create-actions [action-context]
  (->> (:action-definitions action-context)
       (map (fn [[key package action-def]]
              (eval (list 'require `(quote [~package])))
              {key (-> (format "%s/%s" package action-def)
                       symbol
                       eval
                       (assoc :name (name key)))}))
       (apply merge)))

(defn- execute-action [key opts args]
  (let [action (:app (get (create-actions) key))]
    (apply action opts args)))

(defn handle-exception
  "Handle exceptions thrown from CLI actions."
  [e]
  (let [msg (.getMessage e)]
    (if *log-error* (log/error e "action line parse error"))
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
    (str "The following errors occurred while parsing your action:\n\n"
         (s/join \newline errors))))

(defn- action-help
  ([{:keys [name] :as action} skip-action-name?]
   (action-help action skip-action-name? (count name)))
  ([{:keys [name description options] :as action} skip-action-name? max-len]
   (str (if-not skip-action-name?
          (format (str "  %-" (+ 4 max-len) "s") name))
        description
        (if-not (empty? options) \newline)
        (:summary (cli/parse-opts nil options)))))

(defn- action-keys [action-context]
  (or (:action-print-order action-context)
      (map first (:action-definitions action-context))))

(defn- help-msg
  ([action-context actions]
   (help-msg action-context actions nil))
  ([action-context actions action-key]
   (let [{:keys [print-help-fn usage-format-fn action-mode]} action-context
         single-action-mode (= 'single action-mode)
         action-keys (action-keys action-context)
         action-names (->> (vals actions)
                           (map #(-> % :name name)))
         name-len (->> action-names
                       (map count)
                       (apply max))
         action (get actions action-key)
         usage-text (usage-format-fn (if-not single-action-mode action-names))]
     (println usage-text)
     (->> (if action
            (action-help action)
            (->> (map #(get actions %) action-keys)
                 (map #(action-help % single-action-mode name-len))
                 (s/join (str \newline\newline))))
          ((or print-help-fn identity))))))

(defn- parse-single [action-context action arguments single-action-mode?
                     & {:keys [print-errors?]
                        :or {print-errors? true}}]
  (let [{app :app option-defs :options} action
        {:keys [options arguments errors summary]}
        (cli/parse-opts arguments option-defs)]
    (if (nil? app)
      (throw (ex-info (format "Programmer error: missing app: action=%s"
                              action)
                      {:action action})))
    (if errors
      (do
        (if print-errors?
          (->> [(error-msg errors) (action-help action single-action-mode?)]
               (map println)
               doall))
        {:errors errors})
      (apply app (cons options arguments)))))

(defn- parse-multi [action-context actions arguments]
  (let [action-keys (action-keys action-context)
        action-key (keyword (first arguments))
        action (get actions action-key)]
    (if (nil? action)
      (println (help-msg action-context actions action-key))
      (parse-single action-context action (rest arguments) false))))

(defn- parse-global [action-context actions arguments]
  (let [{:keys [global-actions]} action-context]
   (->> global-actions
        (map (fn [action]
               (let [res (parse-single action-context action arguments true
                                       :print-errors? false)]
                 (cond (:global-help res)
                       (do
                         (println (help-msg action-context actions))
                         res)
                       (:global-noop res) res))))
        doall)))

(defn process-arguments
  "Process the action line, which contains the action (action) and arguments
  `args`.

  The action context is a map with actions.  Actions in turn contains what to
  run when an action is given.

  An example of an action context:

```clojure
(defn- create-action-context []
  (multi-action-context
   '((:service com.example service start-server-action)
     (:repl zensols.actioncli repl repl-action))
   :version-option
   (->> (fn [] (println \"version string\"))
        version-option)
   :default-arguments [\"service\" \"-p\" \"8080\"]))
```

  See the [main document page](https://github.com/plandes/clj-actioncli) for
  more info."
  [action-context args]
  (let [arguments (if (empty? args)
                    (:default-arguments action-context)
                    args)
        {:keys [action-mode]} action-context
        single-action-mode? (= action-mode 'single)
        actions (create-actions action-context)
        global-parsed (->> (parse-global action-context actions arguments)
                           (remove nil?))]
    (if-not (empty? global-parsed)
      global-parsed
      (if single-action-mode?
        (parse-single action-context (-> actions vals first) arguments true)
        (parse-multi action-context actions arguments)))))
