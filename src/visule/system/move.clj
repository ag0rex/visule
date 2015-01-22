(ns visule.system.move
  (:require [visule.util :refer [filter-keys]]))

(defn- signed-rand [upper]
  (let [sign (rand-int 2)
        cur (rand-int upper)]
    (if (zero? sign)
      (* cur -1)
      cur)))

(defn- reflect-with-wobble [cur axis wobble]
  (let [new-angle (case axis
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

(defn- move [{x :x y :y}
             {speed :speed direction :direction}]
  (let [[dx dy] (vel-to-dxy speed (deg->rad direction))]
    {:x (+ dx x)
     :y (+ y dy)}))

(defn- hit-bounds? [{x :x y :y}
                    {size :value}
                    {width :width height :height}]
  (let [x-hit (or (>= 0 x)
                  (>= x (- width size)))
        y-hit (or (>= 0 y)
                  (>= y (- height size)))]
    (cond
     (and x-hit y-hit) :xy
     x-hit :x
     y-hit :y)))

(defn- collide
  "Return new direction after collision."
  [pos dir collides size]
  (when collides
    (when-let [axis (hit-bounds? pos size {:width 800 :height 800})]
      (reflect-with-wobble dir axis 0))))

(defn- update [position
               {direction :direction
                collides :collides :as move-component}
               size]
  (let [new-position (move position move-component)]
    (if-let [new-direction (collide new-position direction collides size)]
      {:position new-position :direction new-direction}
      {:position new-position :direction direction})))

(defn- update-entity-position [state key]
  (let [entity (get-in state [:entities key])
        position (:pos entity)
        move-component (:move entity)
        size-comp (:size entity)
        {new-position :position
         new-direction :direction} (update position move-component size-comp)]
    (-> state
        (assoc-in [:entities key :pos] new-position)
        (assoc-in [:entities key :move :direction] new-direction))))

(defn system-move [state]
  (let [movables (filter-keys (:entities state) :move)]
    (reduce update-entity-position state movables)))

(defn- apply-fn [state system-state]
  (system-move state))

(defn init []
  {:fn apply-fn})
