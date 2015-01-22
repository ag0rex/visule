(ns visule.system.size
  (:require [visule.util :refer [filter-keys]]))

(defn- update-entity-size [state key]
  (let [size-fn (get-in state [:entities key :size :fn])]
    (update-in state [:entities key :size :value] size-fn)))

(defn- system-size [state]
  (reduce update-entity-size state (filter-keys (:entities state) :size)))

(defn- apply-fn [state _]
  (system-size state))

(defn init []
  {:fn apply-fn})
