(ns zensols.actioncli.resource-test
  (:require [clojure.test :refer :all])
  (:require [zensols.actioncli.resource :refer :all]))

(deftest test-register
  (testing "register"
    (is (function? (register-resource :data :system-file "data" :system-default "../data")))
    (is (function? (register-resource :runtime-gen :pre-path :data :system-file "db")))))

(deftest test-resource
  (System/clearProperty "zensols.data") ; multiple runs
  (testing "resource path"
    (is (= "../data" (.getPath (resource-path :data))))
    (is (= "../data/db" (.getPath (resource-path :runtime-gen)))))
  (System/setProperty "zensols.data" "/new-data-path")
  (testing "resource path with sys prop"
    (is (= "/new-data-path" (.getPath (resource-path :data))))
    (is (= "/new-data-path/db" (.getPath (resource-path :runtime-gen))))))
