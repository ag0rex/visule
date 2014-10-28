(ns visule.core
  (:require [visule.util :refer (filter-map)]))

;; TODO: Find a better way to return changes from systems, or maybe return a new
;; world state.
(defn- apply-system [state {f :fn system-state :state :as system}]
  (let [{entities-to-merge :merge-entities
         entities-to-remove :remove-entities
         state-to-merge :merge-state} (f state system-state)]
    (-> state
        (merge state-to-merge)
        (update-in [:entities] merge entities-to-merge)
        (update-in [:entities] (partial filter-map #((complement contains?) entities-to-remove (key %)))))))

(defn do-loop [state]
  (if-not (:loop-state state)
    (prn "Stopped!")

    (let [start-time (System/currentTimeMillis)
          state (reduce apply-system state (:systems state))]

      ;; Wait until the next frame.
      (let [frame-time (:frame-time state)
            fps-rest (- frame-time (- (System/currentTimeMillis) start-time))]
        (when (> fps-rest 0)
          (Thread/sleep fps-rest)))

      (recur state))))
