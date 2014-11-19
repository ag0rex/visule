(ns visule.system.interval
  (:require [visule.util :refer [filter-by-comp]]))

(defn- system-interval [interval ts gen-seq apply-fn system-key]
  (let [time-diff (- (System/currentTimeMillis) ts)]
    (when (< interval time-diff)
      {:merge-entities (into {} (take 1 gen-seq))
       :merge-systems {system-key {:fn apply-fn
                                   :state {:interval interval
                                           :gen-seq (drop 1 gen-seq)
                                           :ts (System/currentTimeMillis)
                                           :system-key system-key}}}})))

(defn- apply-fn [_ {interval :interval
                    gen-seq :gen-seq
                    ts :ts
                    system-key :system-key}]
  (system-interval interval ts gen-seq apply-fn system-key))

(defn init [interval-ms gen-fn system-key]
  {:fn apply-fn :state {:interval interval-ms
                        :gen-seq (gen-fn)
                        :ts (System/currentTimeMillis)
                        :system-key system-key}})
