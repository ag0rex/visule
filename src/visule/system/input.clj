(ns visule.system.input)

(defn- pressed? [k input-keys]
  (get input-keys k))

(defn- system-input [input-keys]
  (when (pressed? :q input-keys) {:merge-state {:loop-state false}}))

(defn- apply-fn [_ {input-keys :input-keys-atom :as system-state}]
  (system-input @input-keys))

(defn init [input-keys-atom]
  {:fn apply-fn :state {:input-keys-atom input-keys-atom}})
