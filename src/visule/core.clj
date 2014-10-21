(ns visule.core
  (:require [visule.system.render :refer :all])
  (:import (java.awt Canvas Color GraphicsEnvironment Rectangle)))

(def pressed-keys (atom {}))

(def mouse-pos (atom {}))

(defn on-keypress [ch]
  (swap! pressed-keys assoc (keyword (str ch)) true))

(defn on-keyrelease [ch]
  (swap! pressed-keys assoc (keyword (str ch)) nil))

(defn on-mousemoved [x y]
  (swap! mouse-pos assoc :x x :y y))

(defn pressed? [k]
  (get @pressed-keys k))

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
                                   (min 255 (int (* 255 (/ (+ (Math/abs (- 400 x)) (Math/abs (- 400 y))) 800))))
                                   0
                                   100))

(defn update-ball [entity]
  (let [moved (merge entity (move entity))]
      ;; (if-let [axis (hit-bounds? moved {:width 800 :height 800})]
      ;;   (collide entity axis))
      moved))

(defn draw-ball [{{x :x y :y} :pos size :size :as ball} graphics]
  ;; (.setColor graphics Color/YELLOW)
  ;; (.fillRect graphics 0 0 800 800)
  (.setColor graphics (get-color x y size))
  (.fillOval graphics x y size size))

(defn cursor-draw [{{x :x y :y} :pos size :size :as cursor} graphics]
  (when-not (or (nil? x) (nil? y))
    (.setColor graphics (Color. 0 0 0 50))
    (.fillRect graphics x y size size)))

(defn board-draw [{{x :x y :y} :pos size :size :as cursor} graphics]
  (when-not (or (nil? x) (nil? y))
    (.setColor graphics (Color. 0 0 0))
    (.fillRect graphics x y size size)))

(defn random-objects []
  (cons
   [(keyword (str (rand-int Integer/MAX_VALUE))) {:pos {:x 300 :y 300}
                                                  :vel {:speed (rand-int 5) :direction (- 180 (rand-int 360))}
                                                  :size (+ 100 (rand-int 20))
                                                  :update update-ball
                                                  :draw {:fn draw-ball :shape :ball}
                                                  :grows-on-overlap true}]
   (lazy-seq (random-objects))))

(defn system-follow-mouse [obj-map]
  (zipmap
   (keys obj-map)
   (map #(assoc-in % [:pos] {:x (:x @mouse-pos) :y (:y @mouse-pos)}) (vals obj-map))))

(defn filter-by-comp [objs comp-keyword]
  (filter (comp comp-keyword val) objs))

(defn system-move [state]
  (let [drawable (filter-by-comp (:entities state) :draw)]
    (defn update-map-entry [[key entity]]
      (let [update-fn (:update entity)]
        [key (update-fn entity)]))  
    {:merge-entities (into {} (map update-map-entry drawable))}))

(defn system-grow [state]
  (let [drawable (filter-by-comp (:entities state) :draw)]
    {:merge-entities (into {} (map #(update-in % [1 :size] inc) drawable))}))

(defn system-regen [state]
  (let [drawable (filter-by-comp (:entities state) :draw)]
    (let [too-big (filter #(< 400 (:size (val %))) drawable)]
      {:merge-entities (into {} (take (count too-big) (random-objects)))
       :remove-entities (set (map key too-big))})))

(defn system-input [state]
  (when (pressed? :q) {:merge-state {:loop-state false}}))

(defn filter-map [pred map]
  (select-keys map (for [[k v :as entry] map :when (pred entry)] k)))

;; TODO: Find a better way to return changes, or maybe return a whole
;; new game state.
(defn apply-system [state {f :fn :as system}]
  (let [{entities-to-merge :merge-entities
         entities-to-remove :remove-entities
         state-to-merge :merge-state} (f state)]
    (-> state
        (merge state-to-merge)
        (update-in [:entities] merge entities-to-merge)
        ;; ((fn [state] (do (clojure.pprint/pprint entities-to-remove) state)))
        (update-in [:entities] (partial filter-map #((complement contains?) entities-to-remove (key %)))))))

(defn game-loop [state frame]
  (if-not (:loop-state state) 
    (doto frame
      (.hide)
      (.dispose))
    
    (let [start-time (System/currentTimeMillis)
          state (reduce apply-system state (:systems state))]
      
      ;; Rendering system.
      (system-render
       frame
       (filter-by-comp (:entities state) :board)
       (filter-by-comp (:entities state) :draw)
       (filter-by-comp (:entities state) :cursor))
      
      ;; Wait until the next frame.      
      (let [frame-time (:frame-time state)
            fps-rest (- frame-time (- (System/currentTimeMillis) start-time))]
        (when (> fps-rest 0)
          (Thread/sleep fps-rest)))

      (recur state frame))))

(defn get-frame []
  (setup-frame {:on-keypress on-keypress
                :on-keyrelease on-keyrelease
                :on-mousemoved on-mousemoved}))

(defn init-game-state []
  {:loop-state true
   :frame-time (/ 1000 60)
   :entities (merge
              (into {} (take 100 (random-objects)))
              {:board {:pos {:x 0 :y 0}
                       :size 800
                       :board board-draw}
               ;; :cursor {:pos {:x 50 :y 50}
               ;;          :cursor true
               ;;          :size 100
               ;;          :draw cursor-draw}
               })
   :systems [{:fn system-input}
             {:fn system-move}
             {:fn system-grow}
             {:fn system-regen}
             ;; {:fn system-render :apply-type :side :nodes []}
             ]})

(defn run []
  (let [frame (get-frame)
        state (init-game-state)
        loop (Thread. (fn [] (game-loop state frame)))]
    (.start loop)
    (reset! pressed-keys {})))

(defn stop
  "Simulates a key press in order to stop the game loop."
  []
  (swap! pressed-keys assoc :q true))

(defn reset
  "Restarts the game loop."
  []
  (stop)
  (run))

(defn -main [& args]
  (run))
