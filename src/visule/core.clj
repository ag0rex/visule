(ns visule.core
  (:require visule.system.input
            visule.system.move
            visule.system.size
            visule.system.regen
            visule.system.render
            [visule.util :refer [filter-by-comp]]
            [clojure.tools.namespace.repl :refer [refresh]])
  (:import (java.awt Canvas Color GraphicsEnvironment Rectangle)))

(def input-keys (atom {}))
(def input-mouse (atom {}))

(defn on-keypress [ch]
  (swap! input-keys assoc (keyword (str ch)) true))

(defn on-keyrelease [ch]
  (swap! input-keys assoc (keyword (str ch)) nil))

(defn on-mousemoved [x y]
  (swap! input-mouse assoc :x x :y y))

(defn vel-to-dxy [speed direction]
  [(* speed (Math/cos direction))
   (* speed (Math/sin direction))])

(defn rad->deg [rad]
  (* rad (/ 180 Math/PI)))

(defn deg->rad [deg]
  (* deg (/ Math/PI 180)))

(defn move [{{x :x y :y} :pos {speed :speed direction :direction} :vel :as obj}]
  (let [[dx dy] (vel-to-dxy speed (deg->rad direction))]
    {:pos {:x (+ dx x) :y (+ y dy)}}))

(defn hit-bounds? [{{x :x y :y} :pos size :size} {width :width height :height}]
  (let [x-hit (or (>= 0 x)
                  (>= x (- width size)))
        y-hit (or (>= 0 y)
                  (>= y (- height size)))]
    (cond
     (and x-hit y-hit) :xy
     x-hit :x
     y-hit :y)))

(defn signed-rand [upper]
  (let [sign (rand-int 2)
        cur (rand-int upper)]
    (if (zero? sign)
      (* cur -1)
      cur)))

(defn reflect-with-wobble [cur axis]
  (let [wobble (signed-rand 11)
        new-angle (case axis
                    :x (- 180 cur)
                    :y (- cur)
                    :xy (- cur 180))]
    (+ new-angle wobble)))

(defn collide [{{speed :speed direction :direction} :vel :as obj} axis]
  (let [reflected (assoc obj :vel {:speed speed :direction (reflect-with-wobble direction axis)})]
    (merge reflected (move reflected))))

(defn get-color [x y size] (Color. 155
                                   (min 255
                                        (int (* 255 (/ (+ (Math/abs (- 400 x)) (Math/abs (- 400 y))) 800))))
                                   0
                                   100))

(defn update-ball [entity]
  (let [moved (merge entity (move entity))]
    ;; (if-let [axis (hit-bounds? moved {:width 800 :height 800})]
    ;;   (collide entity axis))
    moved))

(defn draw-ball [{{x :x y :y} :pos {size :value} :size :as ball} graphics]
  ;; (.setColor graphics Color/YELLOW)
  ;; (.fillRect graphics 0 0 800 800)
  (.setColor graphics (get-color x y size))
  (.fillOval graphics x y size size))

(defn cursor-draw [{{x :x y :y} :pos {size :value} :size :as cursor} graphics]
  (when-not (or (nil? x) (nil? y))
    (.setColor graphics (Color. 0 0 0 50))
    (.fillRect graphics x y size size)))

(defn board-draw [{{x :x y :y} :pos {size :value} :size :as cursor} graphics]
  (when-not (or (nil? x) (nil? y))
    (.setColor graphics (Color. 0 0 0))
    (.fillRect graphics x y size size)))

(defn system-follow-mouse [obj-map]
  (zipmap
   (keys obj-map)
   (map #(assoc-in % [:pos] {:x (:x @input-mouse) :y (:y @input-mouse)}) (vals obj-map))))

(defn filter-map [pred map]
  (select-keys map (for [[k v :as entry] map :when (pred entry)] k)))

;; TODO: Find a better way to return changes from systems, or maybe return a new
;; world state.
(defn apply-system [state {f :fn system-state :state :as system}]
  (let [{entities-to-merge :merge-entities
         entities-to-remove :remove-entities
         state-to-merge :merge-state} (f state system-state)]
    (-> state
        (merge state-to-merge)
        (update-in [:entities] merge entities-to-merge)
        (update-in [:entities] (partial filter-map #((complement contains?) entities-to-remove (key %)))))))

(defn do-loop [state]
  (if-not (:loop-state state)
    (prn "STOPPED")
    ;; (doto frame
    ;;   (.hide)
    ;;   (.dispose))

    (let [start-time (System/currentTimeMillis)
          state (reduce apply-system state (:systems state))]

      ;; Wait until the next frame.
      (let [frame-time (:frame-time state)
            fps-rest (- frame-time (- (System/currentTimeMillis) start-time))]
        (when (> fps-rest 0)
          (Thread/sleep fps-rest)))

      (recur state))))

(defn handlers []
  {:on-keypress on-keypress
   :on-keyrelease on-keyrelease
   :on-mousemoved on-mousemoved})

(defn random-objects []
  (cons
   [(keyword (str (rand-int Integer/MAX_VALUE)))
    {:pos {:x 375 :y 375}
     :vel {:speed (+ 3 (rand-int 7)) :direction (- 180 (rand-int 360))}
     :size {:size-fn #(- % 1) :value (+ 50 (rand-int 20))}
     :update update-ball
     :draw {:fn draw-ball :shape :ball}
     :grows-on-overlap true}]
   (lazy-seq (random-objects))))

(defn init-state []
  {:loop-state true
   :frame-time (/ 1000 60)
   :entities (merge
              (into {} (take 200 (random-objects)))
              {:board {:pos {:x 0 :y 0}
                       :size {:size-fn identity :value 800}
                       :board board-draw}
               ;; :cursor {:pos {:x 50 :y 50}
               ;;          :cursor true
               ;;          :size 100
               ;;          :draw cursor-draw}
               })
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
  (refresh :after 'visule.core/run))

(defn -main [& args]
  (run))
