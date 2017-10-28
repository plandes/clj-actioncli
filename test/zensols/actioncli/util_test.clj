(ns zensols.actioncli.util-test
  (:require [clojure.test :refer :all]
            [clojure.tools.logging :as log])
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

(defnprime ^{:foo :bar} test-prime-func
  "Test prime function"
  [a b]
  (+ a b))

(deftest test-prime
  (testing "prime resource"
    (is (= clojure.lang.Atom (type (:init-resource (meta #'test-prime-func)))))
    (is (= false (deref (:init-resource (meta #'test-prime-func)))))
    (is (= :bar (:foo (meta #'test-prime-func))))
    (is (= "Test prime function" (:doc (meta #'test-prime-func))))
    (is (or (-> (meta #'test-prime-func) :init-resource (reset! nil)) true))
    (is (= 3 (test-prime-func 1 2)))
    (is (= 4 (test-prime-func 1 3)))
    (is (true? (-> (meta #'test-prime-func) :init-resource deref)))
    (is (= 4 (test-prime-func 1 3)))))

(defnlock ^{:foo :bar} test-lock-func
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

(defnprime prime1 [o]
  o)

(deftest test-prime-thread
  (testing "prime thread"
    (let [input (set (range 99999))]
      (is (= input (->> input
                        (map (fn [i]
                               (future (prime1 i))))
                        (map deref)
                        set))))))

(defnlock lock1 [o]
  o)

(deftest test-lock-thread
  (testing "lock thread"
    (let [input (range 99999)
          res (->> input
                   (map (fn [i]
                          (future (lock1 i))))
                   (map deref)
                   distinct)]
      (is (= (count res) 1))
      (is (= (first res)
             (->> (meta #'lock1) :init-resource deref))))))

(def ^:private pool-res-inst (atom nil))

(defnpool pool1 [item
                 #(swap! pool-res-inst inc)
                 {:max-total 5
                  ;:max-idle 100
                  ;:min-idle 10
                  }]
  [arg1]
  (+ 1 item))

(deftest test-pool-locking []
  (testing "test pooling"
    (reset! pool-res-inst 0)
    (is (= 0 (->> (range 200)
                  (map #(future (pool1 %)))
                  (map deref)
                  (filter #(> % 6))
                  count)))))

(def ^:private pool-res-inst2 (atom 0))

(defnpool pool2
  [item
   #(swap! pool-res-inst2 inc)]
  [arg1]
  (+ 1 item))

(deftest test-pool-locking []
  (testing "test pooling with default config"
    (reset! pool-res-inst2 0)
    (let [limit 1000]
      (is (> limit
             (->> (range limit)
                  (map #(future (pool2 %)))
                  (map deref)
                  (apply max)))))))
