(ns visule.demo
  (:require [visule.core :refer :all]
            visule.system.input
            visule.system.move
            visule.system.size
            visule.system.regen
            visule.system.render
            visule.system.interval
            visule.system.minim
            [visule.util :refer [filter-by-comp]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]])
  (:import (java.awt Color))
  ;; (:gen-class)
  )

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
     :vel {:speed (+ 1 (rand-int 3)) :direction (- 180 (rand-int 360)) :collides true}
     :size {:fn #(- % 0.1) :value (+ 50 (rand-int 20))}
     :draw {:shape (rand-nth [:circle :square]) :color get-color :z 1}}]
   (lazy-seq (random-objects))))

(defn colored-circles [color z]
  (cons
   [(keyword (str (rand-int Integer/MAX_VALUE)))
    {:pos {:x 300 :y 300}
     :vel {:speed 1 :direction 230 :collides false}
     :size {:fn #(+ % 2) :value 0}
     :draw {:shape :circle :color color :z z}}]
   (lazy-seq (colored-circles color (+ z 2)))))

(defn beat-circles [color z]
  (cons
   [(keyword (str (rand-int Integer/MAX_VALUE)))
    {:pos {:x (+ 320 (rand-int 20))
           :y (+ 320 (rand-int 20))}
     :vel {:speed 3 :direction (quot (System/currentTimeMillis) 2) :collides false}
     :size {:fn #(- % 1) :value 140}
     :draw {:shape :circle :color color :z z}}]
   (lazy-seq (beat-circles (Color. (+ 150 (rand-int 105))
                                   (rand-int 255)
                                   (rand-int 255)
                                   (+ 50 (rand-int 55)))
                           (+ z 2)))))

(defn yellow-magenta [] (interleave (beat-circles (Color. 255 255 255 150) 1)
                                    (beat-circles (Color. 0 0 0 150) 2)))

(defn random-shapes []
  (cons
   [(keyword (str (rand-int Integer/MAX_VALUE)))
    {:pos {:x (rand-int 800) :y (rand-int 800)}
     :vel {:speed 1 :direction (rand-int 360) :collides true}
     :size {:fn identity :value (+ 5 (rand-int 20))}
     :draw {:shape :circle :color (Color. (rand-int 255) (rand-int 255) (rand-int 255) 150) :z 100000}}]
   (lazy-seq (random-shapes))))

(def boo
  {;; :one {:pos {:x 250 :y 250}
   ;;       :vel {:speed 0 :direction 0}
   ;;       :size {:fn identity :value 100}
   ;;       :draw {:shape :circle :color (Color. 55 15 252) :z 10000}}
   :two {:pos {:x 450 :y 250}
         :vel {:speed 0 :direction 0}
         :size {:fn identity :value 100}
         :draw {:shape :square :color (Color. 0 0 0 20) :z 10000}}
   :thr {:pos {:x 250 :y 450}
         :vel {:speed 0 :direction 0}
         :size {:fn identity :value 100}
         :draw {:shape :square :color (Color. 0 0 0 20) :z 10000}}
   :fou {:pos {:x 450 :y 450}
         :vel {:speed 0 :direction 0}
         :size {:fn identity :value 100}
         :draw {:shape :circle :color (Color. 0 0 0 20) :z 10000}}})

(defn init-state []
  {:loop-state true
   :frame-time (/ 1000 60)
   :entities (merge
              ;; (into {} (take 50 (random-objects)))
              ;; (into {} (take 500 (random-shapes)))
              boo
              {:board {:pos {:x 0 :y 0}
                       :size {:fn identity :value 800}
                       :draw {:shape :square :color (Color. 20 10 30) :z 0}}})
   :systems {:input (visule.system.input/init input-keys)
             :size (visule.system.size/init)
             :move (visule.system.move/init)
             :regen (visule.system.regen/init
                     #(and (not= 800 (:value (:size %)))
                           (> 0 (:value (:size %))))
                     (fn [] nil))
             :render (visule.system.render/init handlers)
             ;; :interval (visule.system.interval/init 150 yellow-magenta :interval)
             ;; :interval-2 (visule.system.interval/init 50 random-shapes :interval-2)
             :minim (visule.system.minim/init "/Users/andrei/Music/lilly.mp3" yellow-magenta :minim)}
   :systems-order [:input :size :move :regen :render :minim ;; :interval :interval-2
                   ]})

(defn run []
  (let [state (init-state)
        loop (Thread. (fn [] (do-loop state)))]
    (.start loop)
    (reset! input-keys {})))

(defn stop
  "Simulates a key press in order to stop the world loop."
  []
  (swap! input-keys assoc :q true))

(defn reset
  "Restarts the world loop."
  []
  (stop)
  (refresh :after 'visule.demo/run))

(defn -main [& args]
  (run))
