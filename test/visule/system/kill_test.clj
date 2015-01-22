(ns visule.system.kill-test
  (:require [clojure.test :refer :all]
            [visule.system.kill :refer :all]))

(def state
  {:entities {:a {:boo 1}
              :b {:foo 2}}})

(deftest system-kill-test
  (testing "Kills by predicate."
    (is (= (set (keys (:entities (system-kill state :boo))))
           (set '(:b))))))
