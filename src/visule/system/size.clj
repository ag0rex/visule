(ns visule.system.size)

(defn- filter-by-comp [objs comp-keyword]
  (filter (comp comp-keyword val) objs))

(defn- system-size [state]
  (let [update-size (fn [[_ {{value :value size-fn :fn} :size} :as entity]]
                      (update-in entity [1 :size :value] size-fn))
        drawable (filter-by-comp (:entities state) :draw)]
    {:merge-entities (into {} (map update-size drawable))}))

(defn- apply-fn [state _]
  (system-size state))

(defn init []
  {:fn apply-fn})
