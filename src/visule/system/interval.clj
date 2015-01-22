(ns visule.system.interval)

(defn- system-interval [state interval ts gen-seq system-key]
  (let [time-diff (- (System/currentTimeMillis) ts)]
    (when (< interval time-diff)
      (let [new-entity (first (take 1 gen-seq))]
        (-> state
            (assoc-in [:entities (first new-entity)] (second new-entity))
            (assoc-in [:systems system-key :state :gen-seq] (drop 1 gen-seq))
            (assoc-in [:systems system-key :state :ts] (System/currentTimeMillis)))))))

(defn- apply-fn [state {interval :interval
                        gen-seq :gen-seq
                        ts :ts
                        system-key :system-key}]
  (system-interval state interval ts gen-seq system-key))

(defn init [interval-ms gen-fn system-key]
  {:fn apply-fn :state {:interval interval-ms
                        :gen-seq (gen-fn)
                        :ts (System/currentTimeMillis)
                        :system-key system-key}})
