(ns visule.system.move-test
  (:require [clojure.test :refer :all]
            [visule.system.move :refer :all]))

(def state
  {:entities {:one {:pos {:x 100
                          :y 100}
                    :move {:speed 1
                           :direction 1
                           :collides true}
                    :size {:value 10}}}})

(def state-hitting-bounds
  {:entities {:one {:pos {:x 0
                          :y 100}
                    :move {:speed 5
                           :direction 180
                           :collides true}
                    :size {:value 10}}}})

(deftest system-move-test
  (testing "System move does not mess up state."
    (let [new-state (system-move state)]
      (is (and (get-in new-state [:entities :one :pos])
               (get-in new-state [:entities :one :move :direction])))))

  (testing "Direction changes after hitting bounds."
    (let [new-state (system-move state-hitting-bounds)]
      (is (not= (get-in state [:entities :one :move :direction])
                (get-in new-state [:entities :one :move :direction]))))))
