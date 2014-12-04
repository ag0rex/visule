(ns visule.system.minim
  (:import (ddf.minim Minim)
           (ddf.minim.analysis BeatDetect)))

(defn- system-minim [song beat gen-seq apply-fn system-key]
  (.detect beat (.mix song))
  (when (.isKick beat)
    {:merge-entities (into {} (take 1 gen-seq))
     :merge-systems {system-key {:fn apply-fn
                                 :state {:song song
                                         :beat beat
                                         :gen-seq (drop 1 gen-seq)
                                         :system-key system-key}}}}))

(defn- apply-fn [_ {song :song
                    beat :beat
                    gen-seq :gen-seq
                    system-key :system-key}]
  (system-minim song beat gen-seq apply-fn system-key))

(defn init [song-file gen-fn system-key]
  (let [minim (Minim.)
        song (.loadFile minim song-file 4096)]  
    (.play song)
    (let [beat (BeatDetect. (.bufferSize song) (.sampleRate song))]
      (.setSensitivity beat 10)
      {:fn apply-fn :state {:song song
                            :beat beat
                            :gen-seq (gen-fn)
                            :system-key system-key}})))
