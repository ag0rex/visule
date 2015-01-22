(ns visule.util-test
  (:require [clojure.test :refer :all]
            [visule.util :refer :all]))

(def state
  {:entities {:one {:boo true}
              :two {:foo 1}
              :three {:boo 123}}})

(deftest filter-keys-test
  (testing
      (is (= (set (filter-keys (:entities state) :boo))
             (set '(:one :three))))))
