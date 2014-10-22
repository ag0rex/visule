(ns visule.util)

(defn filter-by-comp [objs comp-keyword]
  (filter (comp comp-keyword val) objs))
