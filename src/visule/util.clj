(ns visule.util)

(defn filter-by-comp [objs comp-keyword]
  (filter (comp comp-keyword val) objs))

(defn remove-keys [map [k & ks]]
  (if ks
    (recur (dissoc map k) ks)
    (dissoc map k)))
