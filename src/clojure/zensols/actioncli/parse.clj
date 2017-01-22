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

(defn- create-actions [action-context]
  (->> (:action-defs action-context)
       (map (fn [[key pkg-parent pkg-child action-def]]
              (let [req (list 'require `(quote (~pkg-parent ~pkg-child)))]
                (eval req)
                {key (symbol (format "%s.%s/%s" pkg-parent
                                     pkg-child action-def))})))
       (apply merge)
       eval
       (merge (:single-actions action-context))
       (map (fn [[k v]]
              {k (assoc v :name (name k))}))
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
          (format (str "%-" (+ 4 max-len) "s") name))
        description \newline
        (:summary (cli/parse-opts nil options))
        (if-not (empty? options) \newline))))

(defn- action-keys [action-context]
  (or (:action-print-order action-context)
      (concat (map first (:action-defs action-context))
              (keys (:single-actions action-context)))))

(defn- help-msg
  ([action-context actions]
   (help-msg action-context actions nil))
  ([action-context actions action-key]
   (let [{:keys [print-help-fn action-mode]} action-context
         action-keys (action-keys action-context)
         name-len (->> (vals actions)
                       (map #(-> % :name name count))
                       (apply max))
         action (get actions action-key)]
     (->> (if action
            (action-help action)
            (->> (map #(get actions %) action-keys)
                 (map #(action-help % (= 'single action-mode) name-len))
                 (s/join \newline)))
          ((or print-help-fn identity))))))

(defn- parse-single [action-context action arguments single-action-mode?
                     & {:keys [print-errors?]
                        :or {print-errors? true}}]
  (let [{app :app option-defs :options} action
        {:keys [options arguments errors summary]}
        (cli/parse-opts arguments option-defs :strict true)]
    (if errors
      (do
        (if print-errors?
          (->> [(error-msg errors) (action-help action single-action-mode?)]
               (map println)
               doall))
        {:errors errors})
      (app options arguments))))

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

  An example of a action context:

```clojure
(defn- create-action-context []
  {:action-defs '((:service com.example service start-server-action)
                   (:repl zensols.actioncli repl repl-action))
   :single-actions {:version version-info-action}
   :default-arguments [\"service\" \"-p\" \"8080\"]})
```

  See the [main document page](https://github.com/plandes/clj-actioncli) for
  more info."
  [action-context & args]
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
