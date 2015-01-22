(ns visule.system.kill
  (:require [visule.util :refer [filter-keys remove-keys]]))

(defn system-kill [{entities :entities :as state} pred]
  (update-in state [:entities] remove-keys (filter-keys entities pred)))

(defn- apply-fn [state {pred :pred :as system-state}]
  (system-kill state pred))

(defn init [pred]
  {:fn apply-fn :state {:pred pred}})
