(ns zensols.actioncli.parse-test
  (:require [clojure.test :refer :all])
  (:require [clojure.java.io :as io]
            [clojure.string :as s])
  (:require [zensols.actioncli.parse :refer :all]))

(def test-action
  {:description "test action"
   :options
   [["-h" "--headless" "start an nREPL server"]
    ["-p" "--port" "database port"
     :required "<port>"
     :missing "Port must be specified"
     :parse-fn #(Integer/parseInt %)
     :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]]
   :app (fn [opts & args]
          ["testcmd" opts args])})

(def test2-action
  {:description "test2 action"
   :options []
   :app (fn [opts & args]
          ["testcmd 2" opts args])})

(defn- create-action-single-context []
  (single-action-context '(zensols.actioncli.parse-test test-action)
                         :version-option
                         (->> (fn [] (println "version string"))
                              version-option)))

(defn- main-single-action-cli [& args]
  (-> (create-action-single-context)
      (process-arguments args)))

(deftest test-parse-single
  (testing "parse single action"
    (is (= '({:global-help true :global-noop true})
           (main-single-action-cli "-h")))
    (is (= "test action
  -h, --headless     start an nREPL server
  -p, --port <port>  database port
"
           (with-out-str (main-single-action-cli "-h"))))
    (is (= '({:global-noop true})
           (main-single-action-cli "-v")))
    (is (= "version string\n"
           (with-out-str (main-single-action-cli "-v"))))
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

(defn- create-action-multi-context []
  (multi-action-context
   '((:tst2 zensols.actioncli.parse-test test2-action)
     (:tst zensols.actioncli.parse-test test-action))
   :version-option
   (->> (fn [] (println "version string"))
        version-option)))

(defn- main-multi-action-cli [& args]
  (-> (create-action-multi-context)
      (process-arguments args)))

(deftest test-parse
  (testing "parse"
    (is (= '({:global-help true :global-noop true})
           (main-multi-action-cli "-h")))
    (is (= "tst2    test2 action

tst     test action
  -h, --headless     start an nREPL server
  -p, --port <port>  database port
"
           (with-out-str (main-multi-action-cli "-h"))))
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
