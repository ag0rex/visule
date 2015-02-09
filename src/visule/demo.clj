(ns visule.demo
  (:require [visule.core :refer :all]
            visule.system.input
            visule.system.move
            visule.system.size
            visule.system.kill
            visule.system.render
            visule.system.interval
            visule.system.minim
            visule.system.one
            [clojure.tools.namespace.repl :refer [refresh refresh-all]])
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
     :move {:speed (+ 1 (rand-int 3)) :direction (- 180 (rand-int 360)) :collides true}
     :size {:fn #(- % 0.1) :value (+ 50 (rand-int 20))}
     :draw {:shape (rand-nth [:circle :square]) :color get-color :z 1}}]
   (lazy-seq (random-objects))))

(defn colored-circles [color z]
  (cons
   [(keyword (str (rand-int Integer/MAX_VALUE)))
    {:pos {:x 300 :y 300}
     :move {:speed 1 :direction 230 :collides false}
     :size {:fn #(+ % 2) :value 0}
     :draw {:shape :circle :color color :z z}}]
   (lazy-seq (colored-circles color (+ z 2)))))

(defn beat-circles [color z]
  (cons
   [(keyword (str (rand-int Integer/MAX_VALUE)))
    {:pos {:x (+ 370 (rand-int 20))
           :y (+ 370 (rand-int 20))}
     :move {:speed 2
           :direction (quot (System/currentTimeMillis) 2)
           :collides false}
     :size {:fn #(- % 1)
            :value 140}
     :draw {:shape :circle
            :color color
            :z z}}]
   (lazy-seq (beat-circles (Color. (+ 150 (rand-int 105))
                                   (+ 150 (rand-int 105))
                                   (rand-int 255)
                                   (+ 0 (rand-int 55)))
                           (+ z 2)))))

(defn yellow-magenta [] (interleave (beat-circles (Color. 255 255 255 150) 1)
                                    (beat-circles (Color. 0 0 0 150) 2)
                                    ))

(defn random-shapes []
  (cons
   [(keyword (str (rand-int Integer/MAX_VALUE)))
    {:pos {:x (rand-int 800) :y (rand-int 800)}
     :move {:speed 1 :direction (rand-int 360) :collides true}
     :size {:fn identity :value (+ 5 (rand-int 20))}
     :draw {:shape :circle :color (Color. (rand-int 255) (rand-int 255) (rand-int 255) 150) :z 100000}}]
   (lazy-seq (random-shapes))))

(defn one []
  (into {}
        (for [x (range 0 10)
              y (range 0 10)]
          {(keyword (str "one-" x y))
           {:pos {:x (* x 100)
                  :y (* y 100)}
            :move {:speed 0
                   :direction 20
                   :collides true}
            :size {:fn #(if (< 5 %) (- % 5) %)
                   :value 140}
            :draw {:shape :circle
                   :color (Color. 200 200 22)
                   :z 1}
            :one true}})))

(defn init-state []
  {:loop-state true
   :frame-time (/ 1000 60)
   :entities (merge
              (one)
              {:board {:pos {:x 400 :y 400}
                       :size {:fn identity :value 800}
                       :draw {:shape :square :color (Color. 120 30 100) :z 0}}})
   :systems {:input (visule.system.input/init input-keys)
             :size (visule.system.size/init)
             :move (visule.system.move/init)
             :kill (visule.system.kill/init
                    #(and (not= 800 (:value (:size %)))
                          (< (:value (:size %)) 0)))
             :render (visule.system.render/init handlers)
             ;;:interval (visule.system.interval/init 100 yellow-magenta :interval)
             :minim (visule.system.minim/init "/Users/andrei/Music/lilly.mp3" yellow-magenta :minim)
             :one (visule.system.one/init)
             }
   :systems-order [:input :render ;;:interval
                   :size :move :kill :minim :one
                   ]})

(defn run []
  (let [state (init-state)]
    (future (do-loop state))
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
