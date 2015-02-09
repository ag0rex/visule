(ns visule.system.one
  (:require [visule.util :refer [filter-keys]]))

(defn- apply-fn [state _]
  (let [low-freq (get-in state [:systems :minim :state :values 3])
        to-update (filter-keys (:entities state) :one)]
    (when (< 200 low-freq)
      (reduce
       #(assoc-in %1 [:entities %2 :size :value] 140)
       state
       to-update))))

(defn init []
  {:fn apply-fn})
