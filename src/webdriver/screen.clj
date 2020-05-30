(ns webdriver.screen
  (:require [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [komcrad-utils.io :as kio]
            [komcrad-utils.wait :refer [wait-for]]
            [clj-file-zip.core :as zip]
            [me.raynes.conch.low-level :as sh]
            [me.raynes.conch :refer [programs with-programs let-programs]])
  (:import (org.openqa.selenium.logging
			   LoggingPreferences LogType)
			 (java.util.logging Level Logger)))

(defn screen-port-available? [n]
  (with-programs [ps]
    (empty?
      (filter #(s/includes? % (str "Xvfb :" n))
              (filter #(s/includes? % "Xvfb")
                      (s/split (ps "aux") #"\n"))))))

(defn screen-port []
 (let [port (+ 50 (rand-int 999999950))]
   (if (screen-port-available? port)
     port
     (screen-port))))

(defn start-screen []
  (let [port (screen-port)
        xvfb (sh/proc "Xvfb" (str ":" port) "-screen" "0" "1920x1080x24")]
    {:xvfb xvfb
     :xvfb-port port}))

(defn stop-screen [m]
  (sh/destroy (:xvfb m)))

(defn start-recorder [m]
  {:xvfb-recorder
   (sh/proc "ffmpeg" "-y" "-f" "x11grab" "-video_size" "1920x1080" "-i"
            (str ":" (:xvfb-port m)) "-codec:v" "libx264" "-r"
            "60" (:vid-out m))})

(defn stop-recorder [m]
  (spit (:in (:xvfb-recorder m)) "q")
  (wait-for #(try (.exitValue (:process (:xvfb-recorder m)))
                  (catch Exception e nil)) 10000 10)
  (sh/destroy (:xvfb-recorder m)))

(defmacro with-screen [[screen] & body]
  `(let [~screen (webdriver.screen/start-screen)]
     (try
       ~@body
       (catch Exception e# (throw e#))
       (finally (webdriver.screen/stop-screen ~screen)))))

(defn stream-to-null [is]
  (future
    (try
      (while true
        (.read is) nil)
      (catch Exception e nil))))

(defmacro with-recorded-screen
  [[screen & {:keys [vid-out]
              :as params
              :or {vid-out "/tmp/webdriver.mp4"}}] & body]
  `(let [~screen (webdriver.screen/start-screen)
         recorder# (webdriver.screen/start-recorder
                     (merge ~screen {:vid-out ~vid-out}))]
     (webdriver.screen/stream-to-null (get-in recorder# [:xvfb-recorder :err]))
     (try
       ~@body
       (catch Exception e# (throw e#))
       (finally 
         (webdriver.screen/stop-recorder recorder#)
         (webdriver.screen/stop-screen ~screen)))))
