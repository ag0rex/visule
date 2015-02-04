(ns visule.system.one)

(defn- apply-fn [state _]
  (let [low-freq (get-in state [:systems :minim :state :values 3])]

    ;; (println low-freq)
    (if (< 200 low-freq)
      (assoc-in state [:entities :one :size :value] 100)
      (assoc-in state [:entities :one :size :value] 0))
    ))

(defn init []
  {:fn apply-fn})
