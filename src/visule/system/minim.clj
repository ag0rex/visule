(ns visule.system.minim
  (:import (ddf.minim Minim)
           (ddf.minim.analysis BeatDetect FFT)))

;; (defn- get-fft-entity [band-no size]
;;   [(symbol (str "band" band-no))
;;    {:pos {:x (* band-no (/ 800 27))
;;           :y (* band-no (/ 800 27))}
;;     :vel {:speed 0 :direction 0}
;;     :size {:fn identity :value (/ size 2)}
;;     :draw {:shape :circle
;;            :color  (Color. (+ 100 (rand-int 50))
;;                            (+ 10 (rand-int 100))
;;                            (+ 20 (rand-int 50))
;;                            (+ 100 (rand-int 0)))
;;            :z 10000}}])

(defn system-minim [state
                    {song :song
                     beat :beat
                     fft :fft
                     system-key :system-key
                     :as system-state}]
  (let [fft-values (vec (map #(.getAvg fft %) (range 0 (.avgSize fft))))]
    (assoc-in state [:systems system-key :state :values] fft-values)))

(defn init [song-file gen-fn system-key]
  (let [minim (Minim.)
        song (.loadFile minim song-file 1024)]
    (.play song)
    (let [beat (BeatDetect. (.bufferSize song) (.sampleRate song))
          fft (FFT. (.bufferSize song) (.sampleRate song))]

      (doto beat
        (.setSensitivity 50))

      (.logAverages fft 60 7)

      {:fn system-minim
       :state {:song song
               :beat beat
               :fft fft
               :system-key system-key}
       :stop-fn #(.close song)})))
