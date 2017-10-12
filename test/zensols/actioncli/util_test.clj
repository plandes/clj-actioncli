(ns zensols.actioncli.util-test
  (:require [clojure.test :refer :all])
  (:require [clojure.java.io :as io]
            [clojure.string :as s])
  (:require [zensols.actioncli.util :refer :all]))

(deftest test-trunc
  (testing "trunc"
    (is (= "12..." (trunc "123456789" 5)))
    (is (= "12345..." (trunc "123456789" 8)))
    (is (= "123456789" (trunc "123456789" 9)))
    (is (= "123456789" (trunc "123456789" 10)))))

(deftest test-parse-macro-args
  (testing "parse macro arguments"
    (is (thrown? clojure.lang.ExceptionInfo (parse-macro-arguments ["doc"])))
    (is (= {:doc "doc"
            :args []
            :forms []}
           (parse-macro-arguments ["doc" []])))
    (is (= {:doc "doc"
            :args [1 2]
            :forms '[(do (println "hi"))]}
           (parse-macro-arguments ["doc" [1 2] '(do (println "hi"))])))))

(def-prime ^{:foo :bar} test-prime-func
  "Test prime function"
  [a b]
  (+ a b))

(deftest test-prime
  (testing "prime resource"
    (is (= clojure.lang.Atom (type (:init-resource (meta #'test-prime-func)))))
    (is (= nil (deref (:init-resource (meta #'test-prime-func)))))
    (is (= :bar (:foo (meta #'test-prime-func))))
    (is (= "Test prime function" (:doc (meta #'test-prime-func))))
    (is (or (-> (meta #'test-prime-func) :init-resource (reset! nil)) true))
    (is (= 3 (test-prime-func 1 2)))
    (is (= 4 (test-prime-func 1 3)))
    (is (true? (-> (meta #'test-prime-func) :init-resource deref)))
    (is (= 4 (test-prime-func 1 3)))))

(def-lockres ^{:foo :bar} test-lock-func
  "Test lock function"
  [a b]
  (+ a b))

(deftest test-lockres
  (testing "lock resource"
    (is (= clojure.lang.Atom (type (:init-resource (meta #'test-lock-func)))))
    (is (= nil (deref (:init-resource (meta #'test-lock-func)))))
    (is (= :bar (:foo (meta #'test-lock-func))))
    (is (= "Test lock function" (:doc (meta #'test-lock-func))))
    (is (or (-> (meta #'test-lock-func) :init-resource (reset! nil)) true))
    (is (= 3 (test-lock-func 1 2)))
    (is (= 3 (test-lock-func 1 3)))
    (is (or (-> (meta #'test-lock-func) :init-resource (reset! nil)) true))
    (is (= 4 (test-lock-func 1 3)))))
