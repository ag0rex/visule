(ns visule.system.render
  (:import (javax.swing JFrame)
           (java.awt Canvas Color GraphicsEnvironment Rectangle)
           (java.awt.event KeyListener MouseMotionListener)))

(defn- setup-frame [{on-keypress :on-keypress
                    on-keyrelease :on-keyrelease
                    on-mousemoved :on-mousemoved}]
  (let [frame (new JFrame "visule")
        ;; ge (GraphicsEnvironment/getLocalGraphicsEnvironment)
        ;; gd (. ge getDefaultScreenDevice)
        ]
    (doto frame
      (.setDefaultCloseOperation JFrame/DISPOSE_ON_CLOSE)
      (.setUndecorated false)
      (.setResizable true))

    ;; (.setFullScreenWindow gd frame)

    (doto frame
      (.setVisible true)
      (.setSize 800 800)
      (.createBufferStrategy 2)

      (.addMouseMotionListener
       (proxy [MouseMotionListener] []
         (mouseDragged [e])
         (mouseMoved [e] (on-mousemoved (.getX e) (.getY e)))))
      
      (.addKeyListener
       (proxy [KeyListener] []
         (keyPressed [e]
           (on-keypress (.getKeyChar e)))
         (keyReleased [e]
           (on-keyrelease (.getKeyChar e)))
         (keyTyped [e])))
      (.validate) ; Makes sure everything inside the frame fits
      (.show))
    frame))

(defn- system-render [frame boards drawable cursors]
  (let [buffer (.getBufferStrategy frame)
        graphics (.getDrawGraphics buffer)]

    (doseq [[obj-key obj] boards]
      ((:board obj) obj graphics))    
    
    (doseq [[obj-key obj] drawable]
      ((:fn (:draw obj)) obj graphics))

    (doseq [[obj-key obj] cursors]
      ((:draw obj) obj graphics))    

    (.dispose graphics)
    (.show buffer)

    {}))

(defn- filter-by-comp [objs comp-keyword]
  (filter (comp comp-keyword val) objs))

(defn- apply-fn [{entities :entities :as state} {frame :frame :as system-state}]
  (let [boards (filter-by-comp entities :board)
        drawable (filter-by-comp entities :draw)
        cursors (filter-by-comp entities :cursor)]
    (system-render frame boards drawable cursors)))

(defn- get-frame [handlers]
  (setup-frame handlers))

(defn init [handlers]
  {:fn apply-fn :state {:frame (get-frame handlers)}})
