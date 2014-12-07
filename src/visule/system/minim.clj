(ns visule.system.minim
  (:import (ddf.minim Minim)
           (ddf.minim.analysis BeatDetect)))

(defn- apply-fn [state
                 {song :song
                  beat :beat
                  gen-seq :gen-seq
                  system-key :system-key
                  :as system-state}]
  (let [system-map (-> state :systems system-key)]
    (.detect beat (.mix song))
    (when (.isRange beat 0 5 6)
      {:merge-entities (into {} (take 1 gen-seq))
       :merge-systems {system-key (update-in system-map [:state :gen-seq] (partial drop 1))}})))

(defn init [song-file gen-fn system-key]
  (let [minim (Minim.)
        song (.loadFile minim song-file 1024)]  
    (.play song)
    (let [beat (BeatDetect. (.bufferSize song) (.sampleRate song))]
      (doto beat
        (.setSensitivity 10))

      (println (.dectectSize beat))
      
      {:fn apply-fn
       :state {:song song
               :beat beat
               :gen-seq (gen-fn)
               :system-key system-key}
       :stop-fn #(.close song)})))
