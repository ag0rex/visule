(ns visule.system.minim-test
  (:require [clojure.test :refer :all]
            [visule.system.minim :refer :all]))

(def state
  {:systems {:minim-test {:state {}}}})

(deftest system-minim-test
  (testing "FFT values go in system state."
    (let [{fn :fn
           stop-fn :stop-fn
           system-state :state} (init "/Users/andrei/Music/lilly.mp3"
                                   (constantly nil)
                                   :minim-test)
           new-state (apply fn [{} system-state])]
      (is (< 0 (count (get-in new-state [:systems :minim-test :state :values]))))
      (stop-fn))))
