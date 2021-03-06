(ns music-examples.osc-out
  (:require [pink.simple :refer :all]
            [pink.live-code :refer :all]
            [pink.config :refer :all]
            [pink.util :refer :all]
            [pink.node :refer :all]
            [pink.envelopes :refer :all]
            [pink.space :refer :all]
            [pink.filters :refer :all]
            [pink.oscillators :refer :all]
            [clojure.string :refer [join]]
            [score.core :refer :all]
            [score.freq :refer :all]
            [score.lc :refer :all]
            [score.euclid :refer :all]
            [score.beats :refer :all] 
            )
  (:import [de.sciss.net OSCClient OSCChannel OSCMessage]
           [java.net InetSocketAddress]
           ))


(def OSC-CLIENT
  (let [c (OSCClient/newUsing OSCChannel/UDP)] 
    (.setTarget c (InetSocketAddress. "127.0.0.1" 10005))
    (.start c)
    c ))

(defn osc-send [target & args]
  ;;(println target)
  (.send OSC-CLIENT (OSCMessage. target (object-array args))))

(defn synth1 
  [dur freq]
  (with-duration (beats dur) 
    (let [e (shared (adsr 0.01 0.05 0.25 0.25))]
      (->
        (sum (blit-saw freq)
             (blit-square (mul freq 2)) ) 
        (zdf-ladder (sum 200 (mul 4000 (exp-env [0.0 1.0 dur 0.001] ))) 5)
        (mul e 0.75)
        (pan 0.0)))))

(defn hex 
  "Predicate that returns if pulse-num item in hex-str beat is 1"
  [hex-str pulse-num]
  (let [pat (hex->pat hex-str) 
        cnt (count pat)
        indx (rem pulse-num cnt)] 
    (== 1 (nth pat indx))))

(defn xosc [phs values]
  (let [p (rem phs 1)
        indx (int (* p (count values)))]
    (nth values indx)))


(defn perf [pulse-num]
  ;; CALLBACK INSTRUMENT
  ;; same style of programming as in Steven's livecode.orc
  ;; Csound live coding framework

  ;(when (hex "a0" pulse-num)
  ;  (osc-send "/i" 1 0.25 440 0.25))

  ;; set to true to hear Pink synth 
  (when true 
    (when (hex "1a" pulse-num)
      (add-afunc (synth1 0.25 
                         (xosc (/ pulse-num 3.0)
                               (map hertz [:a4 :ds3 :a2 ])))))

    (when (hex "2a" pulse-num)
      (add-afunc (synth1 0.25 (hertz :d2))) )

    (when (hex "f0" pulse-num)
      (add-afunc (synth1 0.5 
                         (+ (xosc (/ pulse-num 2.0) (map hertz [:a3 :a4]))
                            (xosc (/ pulse-num 8.0) [0 100 200 400]) 

                            )))))

  )


(comment
  (def af0 
    (synth1 400 (sum (mul (sine 2) 200)  (hertz :e5))))
  (add-afunc af0)
  (remove-afunc af0)


  (add-afunc (apply-stereo mul (synth1 10 (exp-env [0.0 2000.0 10 200] )) 3))
)

(def clock-pulse (long-array 1))

(defn clock []
  (let [last-pulse ^long (aget ^longs clock-pulse 0)
        cur-pulse (long (* (now) 4)) ]
    (when (not= last-pulse cur-pulse)
      (aset ^longs clock-pulse 0 cur-pulse) 
      (#'perf cur-pulse))
    true))




(comment
 
  ;; evaluate this in REPL to get started 
  (start-engine)
  (add-pre-cfunc clock)
  (set-tempo 132)

  )
