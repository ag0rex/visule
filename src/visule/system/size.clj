(ns visule.system.size)

(defn- filter-by-comp [objs comp-keyword]
  (filter (comp comp-keyword val) objs))

(defn- system-grow [state size-fn]
  (let [drawable (filter-by-comp (:entities state) :draw)]
    {:merge-entities (into {} (map #(update-in % [1 :size] size-fn) drawable))}))

(defn- apply-fn [state {size-fn :size-fn :as system-state}]
  (system-grow state size-fn))

(defn init [size-fn]
  {:fn apply-fn :state {:size-fn size-fn}})
