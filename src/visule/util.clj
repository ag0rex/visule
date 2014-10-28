(ns visule.util)

(defn filter-by-comp [objs comp-keyword]
  (filter (comp comp-keyword val) objs))

(defn filter-map [pred map]
  (select-keys map (for [[k v :as entry] map :when (pred entry)] k)))
