(ns zensols.actioncli.parse-test
  (:require [clojure.test :refer :all])
  (:require [clojure.java.io :as io]
            [clojure.string :as s])
  (:require [zensols.actioncli.parse :refer :all]))

(def ^:private test-command
  {:description "test command"
   :options
   [["-h" "--headless" "start an nREPL server"]
    ["-p" "--port <number>" "database port"
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]]
   :app (fn [opts & args]
          ["testcmd" opts args])})

(defn- create-command-context []
  {:single-commands {:tst test-command}})

(defn- main-cli [& args]
  (let [command-context (create-command-context)]
    (apply process-arguments command-context args)))

(deftest test-parse
  (testing "parse"
    (is (= ["testcmd" {} '(())]
           (main-cli "tst")))
    (is (= ["testcmd" {:port 123} '(())]
           (main-cli "tst" "-p" "123")))
    (is (= ["testcmd" {:port 123 :headless true} '(())]
           (main-cli "tst" "-h" "-p" "123")))
    (is (= ["testcmd" {:port 123 :headless true} '(("arg1"))]
           (main-cli "tst" "-h" "-p" "123" "arg1")))))
