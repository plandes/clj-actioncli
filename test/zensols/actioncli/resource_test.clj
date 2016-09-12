(ns zensols.actioncli.resource-test
  (:require [clojure.test :refer :all])
  (:require [clojure.java.io :as io]
            [clojure.string :as s])
  (:require [zensols.actioncli.resource :refer :all]))

(register-resource :data :system-file "data" :system-default "../data")
(register-resource :runtime-gen :pre-path :data :system-file "db")
(register-resource :root-pkg :constant "clojure" :type :resource)

(deftest test-register
  (testing "register"
    (is (function? (register-resource :data :system-file "data" :system-default "../data")))
    (is (function? (register-resource :runtime-gen :pre-path :data :system-file "db")))
    (is (function? (register-resource :root-pkg :constant "clojure" :type :resource)))))

(deftest test-resource
  (System/clearProperty "zensols.data") ; multiple runs
  (testing "resource path"
    (is (= "../data" (.getPath (resource-path :data))))
    (is (= "../data/db" (.getPath (resource-path :runtime-gen)))))
  (System/setProperty "zensols.data" "/new-data-path")
  (testing "resource path with sys prop"
    (is (= "/new-data-path" (.getPath (resource-path :data))))
    (is (= "/new-data-path/db" (.getPath (resource-path :runtime-gen))))))

(deftest test-java-resource
  (is (not (nil? (resource-path :root-pkg))))
  (is (s/starts-with? (.toString (resource-path :root-pkg)) "jar:file:"))
  (is (s/ends-with? (.toString (resource-path :root-pkg))
                                        "org/clojure/clojure/1.8.0/clojure-1.8.0.jar!/clojure"))
  (is (s/starts-with? (.toString (resource-path :root-pkg "string.clj")) "jar:file:"))
  (is (s/ends-with? (.toString (resource-path :root-pkg "string.clj"))
                    "org/clojure/clojure/1.8.0/clojure-1.8.0.jar!/clojure/string.clj"))
  (is (let [content (with-open [reader (io/reader (resource-path :root-pkg "string.clj"))]
                      (slurp reader))]
        (> (count content) 100))))
