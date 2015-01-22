(ns visule.core
  (:require [visule.util :refer [remove-keys]]))

(defn- apply-system [state {f :fn system-state :state :as system}]
  (if-let [new-state (f state system-state)]
    new-state
    state))

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

      ;; (prn state)

      ;; Wait until the next frame.
      (let [frame-time (:frame-time state)
            fps-rest (- frame-time (- (System/currentTimeMillis) start-time))]
        (when (> fps-rest 0)
          (Thread/sleep fps-rest)))
      (recur state))))
