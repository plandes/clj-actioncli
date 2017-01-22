(ns zensols.actioncli.parse-test
  (:require [clojure.test :refer :all])
  (:require [clojure.java.io :as io]
            [clojure.string :as s])
  (:require [zensols.actioncli.parse :refer :all]))

(def ^:private test-command
  {:description "test command"
   :options
   [["-h" "--headless" "start an nREPL server"]
    ["-p" "--port" "database port"
     :required "<port>"
     :missing "Port must be specified"
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]]
   :app (fn [opts & args]
          ["testcmd" opts args])})

(def help-option
  {:description "print help information and exit"
   :options [["-h" "--help"]]
   :app (fn [{:keys [help]} & args]
          (if help {:global-help true}))})

(def version-option
  {:description "print version and exit"
   :options [["-v" "--version"]]
   :app (fn [{:keys [version]} & args]
          (when version
            (println "version: v1")
            {:global-noop true}))})

(defn- create-command-single-context []
  {:single-commands {:tst test-command}
   :global-actions [help-option version-option]
   :action-mode 'single})

(defn- main-single-action-cli [& args]
  (let [command-context (create-command-single-context)]
    (apply process-arguments command-context args)))

(deftest test-parse-single
  (testing "parse single command"
    (is (= '({:global-help true})
           (main-single-action-cli "-h")))
    (is (= '({:global-noop true})
           (main-single-action-cli "-v")))
    (is (= {:errors ["Port must be specified"]}
           (main-single-action-cli)))
    (is (= {:errors ["Missing required argument for \"-p <port>\"" "Port must be specified"]}
           (main-single-action-cli "-p")))
    (is (= ["testcmd" {:port 123} '(())]
           (main-single-action-cli "-p" "123")))
    (is (= ["testcmd" {:port 123 :headless true} '(())]
           (main-single-action-cli "-h" "-p" "123")))
    (is (= ["testcmd" {:port 123 :headless true} '(("arg1"))]
           (main-single-action-cli "-h" "-p" "123" "arg1")))))

(def ^:private test2-command
  {:description "test2 command"
   :options []
   :app (fn [opts & args]
          ["testcmd 2" opts args])})

(defn- create-command-multi-context []
  {:single-commands {:tst test-command
                     :tst2 test2-command}
   :global-actions [help-option version-option]})

(defn- main-multi-action-cli [& args]
  (let [command-context (create-command-multi-context)]
    (apply process-arguments command-context args)))

(deftest test-parse
  (testing "parse"
    (is (= '({:global-help true})
           (main-multi-action-cli "-h")))
    (is (= '({:global-noop true})
           (main-multi-action-cli "-v")))
    (is (= {:errors ["Port must be specified"]}
           (main-multi-action-cli "tst")))
    (is (= ["testcmd" {:port 123} '(())]
           (main-multi-action-cli "tst" "-p" "123")))
    (is (= ["testcmd" {:port 123 :headless true} '(())]
           (main-multi-action-cli "tst" "-h" "-p" "123")))
    (is (= ["testcmd" {:port 123 :headless true} '(("arg1"))]
           (main-multi-action-cli "tst" "-h" "-p" "123" "arg1")))))
