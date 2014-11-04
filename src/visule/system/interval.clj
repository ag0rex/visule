(ns visule.system.interval
  (:require [visule.util :refer [filter-by-comp]]))

(defn- system-interval [interval ts gen-seq apply-fn]
  (let [time-diff (- (System/currentTimeMillis) ts)]
    (when (< interval time-diff)
      {:merge-entities (into {} (take 1 gen-seq))
       :merge-systems {:interval {:fn apply-fn
                                  :state {:interval interval
                                          :gen-seq (drop 1 gen-seq)
                                          :ts (System/currentTimeMillis)}}}})))

(defn- apply-fn [state {interval :interval gen-seq :gen-seq ts :ts :as system-state}]
  (system-interval interval ts gen-seq apply-fn))

(defn init [interval gen-fn]
  {:fn apply-fn :state {:interval interval :gen-seq (gen-fn) :ts (System/currentTimeMillis)}})
