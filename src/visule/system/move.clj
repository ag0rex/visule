(ns visule.system.move
  (:require [visule.util :refer [filter-by-comp]]))

(defn- signed-rand [upper]
  (let [sign (rand-int 2)
        cur (rand-int upper)]
    (if (zero? sign)
      (* cur -1)
      cur)))

(defn- reflect-with-wobble [cur axis]
  (let [wobble (signed-rand 11)
        new-angle (case axis
                    :x (- 180 cur)
                    :y (- cur)
                    :xy (- cur 180))]
    (+ new-angle wobble)))

(defn- vel-to-dxy [speed direction]
  [(* speed (Math/cos direction))
   (* speed (Math/sin direction))])

(defn- rad->deg [rad]
  (* rad (/ 180 Math/PI)))

(defn- deg->rad [deg]
  (* deg (/ Math/PI 180)))

(defn- move [{{x :x y :y} :pos {speed :speed direction :direction} :vel :as obj}]
  (let [[dx dy] (vel-to-dxy speed (deg->rad direction))]
    {:pos {:x (+ dx x) :y (+ y dy)}}))

(defn- collide [{{direction :direction} :vel :as obj} axis]
  (let [reflected (update-in obj [:vel :direction] #(reflect-with-wobble % axis))]
    (merge reflected (move reflected))))

(defn- hit-bounds? [{{x :x y :y} :pos
                     {size :value} :size}
                    {width :width height :height}]
  (let [x-hit (or (>= 0 x)
                  (>= x (- width size)))
        y-hit (or (>= 0 y)
                  (>= y (- height size)))]
    (cond
     (and x-hit y-hit) :xy
     x-hit :x
     y-hit :y)))

(defn- update [{{collides :collides} :vel :as entity}]
  (let [moved (merge entity (move entity))]
    (if collides
      (if-let [axis (hit-bounds? moved {:width 800 :height 800})]
        (collide entity axis)
        moved)
      moved)))

(defn- system-move [state]
  (let [update-map-entry (fn [[key entity]]
                           [key (update entity)])
        movable (filter-by-comp (:entities state) :vel)]
    {:merge-entities (into {} (map update-map-entry movable))}))

(defn- apply-fn [state system-state]
  (system-move state))

(defn init []
  {:fn apply-fn})
