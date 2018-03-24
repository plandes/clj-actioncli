(ns zensols.actioncli.resource-test
  (:require [clojure.test :refer :all])
  (:require [clojure.java.io :as io]
            [clojure.string :as s])
  (:require [zensols.actioncli.resource :refer :all]))

(def root-pkg-pat
  "^jar:file:.+pool/pool/[0-9.]+/pool-[0-9.]+\\.jar!/pool")

(defn- init-sysprop []
  ;; multiple runs
  (System/clearProperty "zensols.data"))

(defn- init-register []
  (register-resource :data :system-file "data" :system-default "../data")
  (register-resource :runtime-gen :pre-path :data :system-file "db"))

(deftest test-resource
  (init-sysprop)
  (init-register)
  (testing "resource path"
    (is (= "../data" (.getPath (resource-path :data))))
    (is (= "../data/db" (.getPath (resource-path :runtime-gen)))))
  (System/setProperty "zensols.data" "/new-data-path")
  (testing "resource path with sys prop"
    (is (= "/new-data-path" (.getPath (resource-path :data))))
    (is (= "/new-data-path/db" (.getPath (resource-path :runtime-gen))))))

(deftest test-java-resource
  (init-sysprop)
  (register-resource :root-pkg :constant "pool" :type :resource)
  (is (not (nil? (resource-path :root-pkg))))
  (is (re-matches (re-pattern root-pkg-pat)
                  (.toString (resource-path :root-pkg))))
  (is (re-matches (re-pattern (str root-pkg-pat "/core.clj"))
                  (.toString (resource-path :root-pkg "core.clj"))))
  (is (let [content
            (with-open [reader (io/reader (resource-path :root-pkg "core.clj"))]
              (slurp reader))]
        (> (count content) 100))))

(deftest test-prepath
  (init-sysprop)
  (init-register)
  (is (not (nil? (resource-path :runtime-gen))))
  (is (instance? java.io.File (resource-path :runtime-gen)))
  (is (= "../data/db/dir2" (.getPath (resource-path :runtime-gen "dir2"))))
  (System/setProperty "zensols.data" "/new-data-path")
  (is (= "/new-data-path/db/dir3" (.getPath (resource-path :runtime-gen "dir3")))))

(deftest test-func
  (init-sysprop)
  (register-resource :func-dir
                     :function (fn afn
                                 ([] (afn "nofile"))
                                 ([file]
                                  [:moredata (str "/somepath/" file)])))
  (is (= [:moredata "/somepath/nofile"] (resource-path :func-dir)))
  (is (= [:moredata "/somepath/pos"] (resource-path :func-dir "pos")))
  (register-resource :func-dir
                     :function (fn
                                 ([] (io/file "no-file-for-you"))
                                 ([file]
                                  (io/file "/another/path" file))))
  (is (= (io/file "/another/path/pos") (resource-path :func-dir "pos")))
  (is (= (io/file "no-file-for-you") (resource-path :func-dir))))

(deftest test-with-resources
  (init-sysprop)
  (register-resource :data :system-file "../more-data")
  (register-resource :runtime-gen :pre-path :data :system-file "db")
  (is (= (io/file "../more-data/db") (resource-path :runtime-gen)))
  (with-resources
    (register-resource :data :system-file "../less-data")
    (is (= (io/file "../less-data/db") (resource-path :runtime-gen))))
  (is (= (io/file "../more-data/db") (resource-path :runtime-gen))))
