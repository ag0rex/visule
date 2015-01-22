(ns visule.util)

(defn filter-keys [entities pred]
  (reduce (fn [acc x] (if (pred (val x)) (conj acc (key x)) acc)) '() entities))

(defn remove-keys [map [k & ks]]
  (if ks
    (recur (dissoc map k) ks)
    (dissoc map k)))
