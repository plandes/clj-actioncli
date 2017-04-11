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

(def ^:dynamic *rethrow-error*
  "When not dying on error with [[*dump-jvm-on-error*]] also don't rethrow the
  exception in [[handle-exception]]."
  true)

(def ^:dynamic *parse-context*
  "This context is bound when calling action bound functions.  This is useful
  when other functions in this namespace need to be called with parse
  information.

  This context is a map with the following keys:

  * **:action-context** the context created with [[multi-action-context]]
  or [[single-action-context]]
  * **:arguments** the arguments passed to the parse function
  * **:options** the parsed options
  * **:single-action-mode?** whether the action context was created
  with [[multi-action-context]] or [[single-action-context]]"
  nil)

(defa- program-name-inst "prog")

(defn program-name
  "Return the program name used for info/error message.  This is set
  with [[set-program-name]]."
  []
  @program-name-inst)

(defn- program-fmt []
  (let [name (program-name)]
    (if name
      (str name ": ")
      "")))

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
                  (or (program-name) "")
                  (if-not action-names
                    ""
                    (->> action-names
                         (s/join "|")
                         (format " <%s>"))))))))

(defn- action-keys [action-context]
  (or (:action-print-order action-context)
      (map first (:action-definitions action-context))))

(defn- action-help
  ([{:keys [name] :as action} skip-action-name?]
   (action-help action skip-action-name? (count name)))
  ([{:keys [name description options] :as action} skip-action-name? max-len]
   (str (if-not skip-action-name?
          (format (str "  %-" (+ 4 max-len) "s") name))
        description
        (if-not (empty? options) \newline)
        (:summary (cli/parse-opts nil options)))))

(defn help-message
  "Generate the help message text and return it.
  The **action-key** is a keyword of the passed as a command
  to [[multi-action-context]].  By default the usage text is included unless
  the `:usage` key is `false`.  If the `:usage` is the symbol `only` then only
  create the usage text and not the action/parameters."
  [& {:keys [action-key usage]
      :or {usage true}}]
  (let [{:keys [action-context actions]} *parse-context*
        {:keys [print-help-fn usage-format-fn action-mode]} action-context
        single-action-mode (= 'single action-mode)
        action-keys (action-keys action-context)
        action-names (->> (vals actions)
                          (map #(-> % :name name)))
        name-len (->> action-names
                      (map count)
                      (apply max))
        action (get actions action-key)]
    (if usage
      (->> (if-not single-action-mode action-names)
           usage-format-fn
           println))
    (if-not (= usage 'only)
     (->> (if action
            (action-help action single-action-mode name-len)
            (->> (map #(get actions %) action-keys)
                 (map #(action-help % single-action-mode name-len))
                 (s/join (str \newline\newline))))
          ((or print-help-fn identity))))))

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
  single action usage messages
* **:no-action-fn** a function to call when no action is given, defaults to
  printing the contents of [[help-message]]; set to `nil` to continue
  processing without an action"
  [actions &
   {:keys [global-actions help-option version-option
           action-print-order print-help-fn usage-format-fn
           no-action-fn]
    :or {help-option (help-option)
         no-action-fn (fn [] (println (help-message)))}}]
  (merge (if action-print-order {:action-print-order action-print-order})
         (if print-help-fn {:print-help-fn print-help-fn})
         {:usage-format-fn (or usage-format-fn (create-default-usage-format))}
         {:action-definitions actions
          :global-actions (concat global-actions
                                  (if help-option [help-option])
                                  (if version-option [version-option]))
          
          :no-action-fn no-action-fn
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

(defn create-actions [action-context]
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
  (log/debugf "handle exception: <%s>" e)
  (let [msg (.getMessage e)]
    (if *log-error* (log/error e "action line parse error"))
    (if (instance? java.io.FileNotFoundException e)
      (binding [*out* *err*]
        (println (format "%sio error: %s" (program-fmt) msg)))
      (binding [*out* *err*]
        (println (format "%serror: %s"
                         (program-fmt)
                         (if ex-data
                           (.getMessage e)
                           (.toString e)))))))
  (log/debugf "dumping JVM: %s, rethrow: %s"
              *dump-jvm-on-error* *rethrow-error*)
  (if *dump-jvm-on-error*
    (System/exit 1)
    (if *rethrow-error* (throw e))))

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
    (str (program-fmt) (first errors))
    (str "The following errors occurred while parsing your action:\n\n"
         (s/join \newline errors))))

(defn- parse-single [action-context actions action action-key
                     arguments single-action-mode?
                     & {:keys [print-errors?]
                        :or {print-errors? true}}]
  (let [{app :app option-defs :options} action
        {:keys [options arguments errors summary]}
        (cli/parse-opts arguments option-defs)
        errors (if action
                 errors
                 (cons (if action-key
                         (format "No such action: %s" (name action-key)))
                       errors))]
    (binding [*parse-context* {:action-context action-context
                               :actions actions
                               :action action
                               :options options
                               :arguments arguments
                               :single-action-mode? single-action-mode?}]
      (if (and (not errors) (nil? app))
        (-> (format "Programmer error: missing app: action=%s"
                                action)
            (ex-info {:action action})
            throw))
      (if errors
        (do
          (if print-errors?
            (->> (concat [(error-msg errors)]
                         (if action [(action-help action single-action-mode?)]))
                 (map println)
                 doall))
          {:errors errors})
        (apply app (cons options arguments))))))

(defn- parse-multi [action-context actions arguments]
  (let [action-keys (action-keys action-context)
        action-key (keyword (first arguments))
        action (get actions action-key)
        {:keys [no-action-fn]} action-context]
    (if (and (nil? action) no-action-fn)
      (binding [*parse-context* {:action-context action-context
                                 :actions actions
                                 :argument arguments
                                 :single-action-mode? false}]
        (no-action-fn))
      (parse-single action-context actions action action-key (rest arguments) false))))

(defn- parse-global [action-context actions arguments]
  (let [{:keys [global-actions]} action-context]
   (->> global-actions
        (map (fn [action]
               (let [res (parse-single actions action-context
                                       action 'none arguments
                                       true :print-errors? false)]
                 (cond (:global-help res)
                       (binding [*parse-context* {:action-context action-context
                                                  :actions actions}]
                         (println (help-message))
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
        (let [action (-> actions vals first)]
          (parse-single action-context actions action 'none arguments true))
        (parse-multi action-context actions arguments)))))
