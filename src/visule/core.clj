(ns visule.core
  (:require [visule.util :refer [remove-keys]]))

;; TODO: Find a better way to return changes from systems, or maybe return a new
;; world state.
(defn- apply-system [state {f :fn system-state :state :as system}]
  (let [{entities-to-merge :merge-entities
         entities-to-remove :remove-entities
         state-to-merge :merge-state
         systems-to-merge :merge-systems} (f state system-state)]
    (-> state
        (merge state-to-merge)
        (update-in [:entities] merge entities-to-merge)
        (update-in [:entities] remove-keys (seq entities-to-remove))
        (update-in [:systems] merge systems-to-merge))))

(defn do-loop [state]
  (if-not (:loop-state state)
    ;; Call stop functions for all systems.
    (let [systems (:systems state)]
      (doseq [[_ system-state] systems]
        (when-let [stop-fn (:stop-fn system-state)]
          (stop-fn))))

    ;; Or apply systems on world state.
    (let [start-time (System/currentTimeMillis)
          ordered-systems (map #(% (:systems state)) (:systems-order state))
          state (reduce apply-system state ordered-systems)]

      ;; Wait until the next frame.
      (let [frame-time (:frame-time state)
            fps-rest (- frame-time (- (System/currentTimeMillis) start-time))]
        (when (> fps-rest 0)
          (Thread/sleep fps-rest)))

      (recur state))))
