(ns visule.system.size)

(defn- filter-by-comp [objs comp-keyword]
  (filter (comp comp-keyword val) objs))

(defn- system-size [state]
  (defn update-size [[_ {{value :value size-fn :size-fn} :size} :as entity]]
    (update-in entity [1 :size :value] size-fn))
  
  (let [drawable (filter-by-comp (:entities state) :draw)]
    {:merge-entities (into {} (map update-size drawable))}))

(defn- apply-fn [state _]
  (system-size state))

(defn init []
  {:fn apply-fn})
