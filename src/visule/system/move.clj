(ns visule.system.move
  (:require [visule.util :refer [filter-by-comp]]))

(defn- system-move [state]
  (let [drawable (filter-by-comp (:entities state) :draw)]
    (defn update-map-entry [[key entity]]
      (let [update-fn (:update entity)]
        [key (update-fn entity)]))  
    {:merge-entities (into {} (map update-map-entry drawable))}))

(defn- apply-fn [state _]
  (system-move state))

(defn init []
  {:fn apply-fn})
