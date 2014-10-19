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
                                   (int (* 255 (/ (+ (Math/abs (- 400 x)) (Math/abs (- 400 y))) 800)))
                                   0
                                   100))

(defn update-ball [entity]
  (let [moved (merge entity (move entity))]
      (if-let [axis (hit-bounds? moved {:width 800 :height 800})]
        (collide entity axis)
        moved)))

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
    (.setColor graphics (Color. 255 255 255))
    (.fillRect graphics x y size size)))

(defn random-objects []
  (cons
   [(keyword (str (rand-int Integer/MAX_VALUE))) {:pos {:x 400 :y 400}
                                                  :vel {:speed (+ 2 (rand-int 2)) :direction (- 180 (rand-int 360))}
                                                  :size (+ 100 (rand-int 20))
                                                  :update update-ball
                                                  :draw draw-ball
                                                  :grows-on-overlap true}]
   (lazy-seq (random-objects))))

(defn system-follow-mouse [obj-map]
  (zipmap
   (keys obj-map)
   (map #(assoc-in % [:pos] {:x (:x @mouse-pos) :y (:y @mouse-pos)}) (vals obj-map))))

(defn system-overlaps-cursor [cursor-seq obj-seq]
  (defn get-rectangle-from-obj [{{x :x y :y} :pos size :size}]
    (Rectangle. x y size size))

  (defn intersects-rectangles [rect rects-to-intersect]
    (some #(.intersects rect %) rects-to-intersect))
  
  ;; Get objs that overlap cursors.
  (let [obj-map (into {} obj-seq)
        valid-cursors (filter (comp :x :pos) (vals cursor-seq))
        cursors-rects (map get-rectangle-from-obj valid-cursors)
        objs-rects (zipmap (keys obj-map) (map get-rectangle-from-obj (vals obj-map)))
        objs-overlap (filter (fn [[key rect]] (intersects-rectangles rect cursors-rects)) objs-rects)
        overlap-keys (keys objs-overlap)]
    (zipmap
     overlap-keys
     (map #(update-in (% obj-map) [:size] inc) overlap-keys))))

(defn system-move [drawable]
  (defn update-map-entry [[key entity]]
    (let [update-fn (:update entity)]
      [key (update-fn entity)]))
  
  (into {} (map update-map-entry drawable)))

(defn system-grow [drawable]
  (into {} (map #(update-in % [1 :size] dec) drawable)))

(defn system-regen [drawable]
  (let [too-big (filter #(> 10 (:size (val %))) drawable)
        marked-to-kill (map #(assoc-in % [1 :-kill] true) too-big)]
    (into {} (merge (into {} (take (count too-big) (random-objects)))
                    (into {} marked-to-kill)))))

(defn system-cleanup [entities]
  (into {} (filter #((complement contains?) (val %) :-kill) entities)))

(def game-state (atom {:loop-state true
                       :update-state true
                       :frame-time (/ 1000 60)
                       :entities (merge
                              (into {} (take 30 (random-objects)))
                              {:board {:pos {:x 0 :y 0}
                                       :size 800
                                       :board board-draw}
                               ;; :cursor {:pos {:x 50 :y 50}
                               ;;          :cursor true
                               ;;          :size 100
                               ;;          :draw cursor-draw}
                               })
                       :systems [{:fn system-move :apply-type :merge :nodes [:draw]}
                                 {:fn system-grow :apply-type :merge :nodes [:draw]}
                                 {:fn system-regen :apply-type :merge :nodes [:draw]}
                                 {:fn system-cleanup :apply-type :reset :nodes :all}
                                 ;; {:fn system-render :apply-type :side :nodes []}
                                 ]}))

(defn filter-by-comp [objs comp-keyword]
  (filter (comp comp-keyword val) objs))

(defn objs-by-comp [comp-keyword]
  (filter-by-comp (:entities @game-state) comp-keyword))

(defn change-state [k v]
  (swap! game-state #(assoc % k v)))

(defn system-apply-merge [system-fn args]
  (change-state :entities
                (merge
                 (:entities @game-state)
                 (apply system-fn args))))

(defn system-apply-reset
  ([system-fn] (system-apply-reset system-fn []))
  ([system-fn args] (change-state :entities (apply system-fn args))))

(defn game-loop [frame]
  (if-not (:loop-state @game-state) 
    (doto frame
      (.hide)
      (.dispose))
    
    (let [start-time (System/currentTimeMillis)]

      ;; User input.
      (when (pressed? :q)
        (change-state :loop-state false))
      (when (pressed? :s)
        (change-state :update-state (not (:update-state @game-state))))

      ;; Run all systems.
      (let [systems (:systems @game-state)]
        (doseq [{f :fn mode :apply-type nodes-keys :nodes} systems]
          (let [nodes (if (= :all nodes-keys) [(:entities @game-state)] (map objs-by-comp nodes-keys))]
            (case mode
              :merge (system-apply-merge f nodes)
              :reset (system-apply-reset f nodes)))))
                  
      ;; Rendering system.
      (system-render
       frame
       (objs-by-comp :board)
       (objs-by-comp :draw)
       (objs-by-comp :cursor))
      
      ;;wait until the next frame
      (let [frame-time (:frame-time @game-state)
            fps-rest (- frame-time (- (System/currentTimeMillis) start-time))]
        (when (> fps-rest 0)
          (Thread/sleep fps-rest)))

      (recur frame))))

(defn get-frame []
  (setup-frame {:on-keypress on-keypress
                :on-keyrelease on-keyrelease
                :on-mousemoved on-mousemoved}))

(defn run []
  (let [frame (get-frame)]
    (change-state :loop-state true)
    (reset! pressed-keys {})
    (let [game (Thread. (fn [] (game-loop frame)))]
      (.start game))
    ;;(game-loop frame)
    ))

(defn stop []
  (change-state :loop-state false))

(defn reset []
  (stop)
  (run))

(defn -main [& args]
  (run))
