(ns visule.system.minim
  (:import (ddf.minim Minim)
           (ddf.minim.analysis BeatDetect FFT)
           (java.awt Color)))

(defn- get-fft-entity [band-no size]
  [(symbol (str "band" band-no))
   {:pos {:x (* band-no (/ 800 27)) :y 50}
    :vel {:speed 0 :direction 0}
    :size {:fn identity :value size}
    :draw {:shape :square :color (Color. 200 200 0 100) :z 10000}}])

(defn- apply-fn [state
                 {song :song
                  beat :beat
                  fft :fft
                  gen-seq :gen-seq
                  system-key :system-key
                  :as system-state}]

  (.forward fft (.mix song))
  
  (.detect beat (.mix song))
  
  (let [system-map (-> state :systems system-key)
        fft-ents (map #(get-fft-entity % (.getAvg fft %))
                      (range 0 (.avgSize fft)))]

    ;; (println fft-ent)
    ;; (when (.isRange beat 0 5 6))
    
    (let [ents (if (.isRange beat 1 2 2) (concat (take 1 gen-seq) fft-ents) fft-ents)]
      {:merge-entities (into {} ents)
       :merge-systems {system-key (update-in system-map [:state :gen-seq] (partial drop 1))}})))

(defn init [song-file gen-fn system-key]
  (let [minim (Minim.)
        song (.loadFile minim song-file 1024)]
    (.play song)
    (let [beat (BeatDetect. (.bufferSize song) (.sampleRate song))
          fft (FFT. (.bufferSize song) (.sampleRate song))]

      (doto beat
        (.setSensitivity 10))

      (.logAverages fft 60 3)
      
      (println (.dectectSize beat))
      
      {:fn apply-fn
       :state {:song song
               :beat beat
               :fft fft
               :gen-seq (gen-fn)
               :system-key system-key}
       :stop-fn #(.close song)})))
