(ns visule.system.regen
  (:require [visule.util :refer [filter-by-comp]]))

(defn- system-regen [objs pred gen-fn]
  (let [to-remove (filter #(pred (val %)) objs)]
    {:merge-entities (into {} (take (count to-remove) (gen-fn)))
     :remove-entities (set (map key to-remove))}))

(defn- apply-fn [{entities :entities :as state}
                 {pred :pred gen-fn :gen-fn :as system-state}]
  (let [drawable (filter-by-comp entities :draw)]
    (system-regen drawable pred gen-fn)))

(defn init [pred-remove gen-fn]
  {:fn apply-fn :state {:pred pred-remove :gen-fn gen-fn}})
