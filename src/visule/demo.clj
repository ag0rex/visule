(ns visule.demo
  (:require [visule.core :refer :all]
            visule.system.input
            visule.system.move
            visule.system.size
            visule.system.regen
            visule.system.render
            [visule.util :refer [filter-by-comp filter-map]]
            [clojure.tools.namespace.repl :refer [refresh]])
  (:import (java.awt Color)))

(def input-keys (atom {}))
(def input-mouse (atom {}))

(defn on-keypress [ch]
  (swap! input-keys assoc (keyword (str ch)) true))

(defn on-keyrelease [ch]
  (swap! input-keys assoc (keyword (str ch)) nil))

(defn on-mousemoved [x y]
  (swap! input-mouse assoc :x x :y y))

(defn handlers []
  {:on-keypress on-keypress
   :on-keyrelease on-keyrelease
   :on-mousemoved on-mousemoved})

(defn get-color [{{x :x y :y} :pos :as entity}]
  (Color. 155
          (min 255
               (int (* 255 (/ (+ (Math/abs (- 400 x)) (Math/abs (- 400 y))) 800))))
          0
          100))

(defn random-objects []
  (cons
   [(keyword (str (rand-int Integer/MAX_VALUE)))
    {:pos {:x 375 :y 375}
     :vel {:speed (+ 1 (rand-int 3)) :direction (- 180 (rand-int 360))}
     :size {:fn #(- % 0.1) :value (+ 50 (rand-int 20))}
     :draw {:shape (rand-nth [:circle :square]) :color get-color :z 1}}]
   (lazy-seq (random-objects))))

(defn init-state []
  {:loop-state true
   :frame-time (/ 1000 60)
   :entities (merge
              (into {} (take 200 (random-objects)))
              {:board {:pos {:x 0 :y 0}
                       :size {:fn identity :value 800}
                       :draw {:shape :square :color (Color. 0 0 0) :z 0}}})
   :systems [(visule.system.input/init input-keys)
             (visule.system.size/init)
             (visule.system.move/init)
             (visule.system.regen/init #(< (:value (:size %)) 1) random-objects)
             (visule.system.render/init (handlers))]})

(defn run []
  (let [state (init-state)
        loop (Thread. (fn [] (do-loop state)))]
    (.start loop)
    (reset! input-keys {})))

(defn stop
  "Simulates a key press in order to stop the world loop."
  ;; TODO: Broken.
  []
  (swap! input-keys assoc :q true))

(defn reset
  "Restarts the world loop."
  []
  (stop)
  (refresh :after 'visule.demo/run))

(defn -main [& args]
  (run))
