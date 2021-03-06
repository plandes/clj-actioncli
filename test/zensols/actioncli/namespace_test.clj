(ns zensols.actioncli.namespace-test
  (:require [clojure.test :refer :all])
  (:require [zensols.actioncli.ns :refer :all]))

(deftest test-resource
  (testing "invoke via existing ns"
    (with-other-ns 'clojure.string
      (clojure.test/is (= "Dog" (capitalize "dog")))))
  (testing "temporary namespace"
    (with-temp-ns 'some.non.existing.ns
      (def some-new-var 'aval)
      (clojure.test/is (= "Dog" (clojure.string/capitalize "dog")))
      (clojure.test/is (resolve 'some-new-var)))
    (is (not (resolve 'some-new-var))))
  (testing "requires in ns"
    (is (= "Dog"
           (with-ns [(:require [clojure.string :as s])]
             (s/capitalize "dog")))))
  (testing "context in in"
    (let [abind 'someval]
      (is (= "Someval"
             (with-context-ns
                 abind
                 [(:require [clojure.string :as s])]
               (s/capitalize (str (name abind)))))))))

