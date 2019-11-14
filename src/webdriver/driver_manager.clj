(ns webdriver.driver-manager
  (:gen-class)
  (:require [org.httpkit.client :as http]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [komcrad-utils.io :as kio]
            [me.raynes.fs.compression :refer [gunzip untar]]
            [komcrad-utils.wait :refer [wait-for]]
            [clj-file-zip.core :as zip]
            [me.raynes.conch.low-level :as sh])
  (:import (org.openqa.selenium.logging
			   LoggingPreferences LogType)
			 (java.util.logging Level Logger)))

(defonce latest-chrome-version "78.0.3904.70")
(defonce latest-gecko-version "0.26.0")

(defn- mkdownload-dir [m]
  (let [dir (:download-dir m)]
    (if dir
      (do (.mkdir (clojure.java.io/file dir))
          (if (not (.isDirectory (clojure.java.io/file dir)))
            (throw (Exception. ":download-dir must be a folder."))
            (.getCanonicalPath (clojure.java.io/file dir))))
      (System/getProperty "user.dir"))))

(defn host-os []
  (let [os (System/getProperty "os.name")]
    (cond
      (s/includes? os "Linux") :linux
      (s/includes? os "Windows") :windows)))

(defn latest-driver-version [m]
  (cond (= :firefox (:driver-type m)) latest-gecko-version
        (= :chrome (:driver-type m)) latest-chrome-version
        :else (latest-driver-version (merge m {:driver-type :chrome}))))

(defn driver-version [m]
  (if (:version m)
    (:version m)
    (latest-driver-version m)))

(defn driver-download-url [m]
  (cond
    (= (:driver-type m) :firefox)
    (str "https://github.com/mozilla/geckodriver/releases/download/v"
         (driver-version m) "/geckodriver-v" (driver-version m)
         "-linux64.tar.gz")
    (= (:driver-type m) :chrome)
    (str "https://chromedriver.storage.googleapis.com/" (driver-version m)
         "/chromedriver_linux64.zip")
    :else (driver-download-url (merge m {:driver-type :chrome}))))

(defn host-os []
  (let [os (System/getProperty "os.name")]
    (cond
      (s/includes? os "Linux") :linux
      (s/includes? os "Windows") :windows)))

(defn driver-download-url [m]
  (cond
    (= (:driver-type m) :firefox)
    (str "https://github.com/mozilla"
         "/geckodriver/releases/download/v"
         (driver-version m) "/geckodriver-v"
         (driver-version m)
         "-linux64.tar.gz")
    (= (:driver-type m) :chrome)
    (str "https://chromedriver.storage.googleapis.com/"
         (driver-version m) "/chromedriver_linux64.zip")
    :else (driver-download-url (merge m {:driver-type :chrome}))))

(defn driver-parent [m]
  (let [base (if (:driver-download-dir m)
               (:driver-download-dir m)
               "./.webdrivers")
        type (cond
               (= (:driver-type m) :firefox) "/geckodriver"
               (= (:driver-type m) :chrome) "/chromedriver"
               :else "/chromedriver")
        arch (cond
               (= (host-os) :linux) "/linux64"
               (= (host-os) :windows) "/win32")
        version (str "/" (driver-version m) "/")]
    (str base type arch version)))

(defn download-driver [m]
  (let [url (driver-download-url m)
        file-name (last (s/split url #"/"))
        down-dir (driver-parent m)
        res (http/get (driver-download-url m))]
    (if (= 404 (:status @res))
      (throw (Exception. (str "Could not find driver " (:driver-type m)
                              " version " (driver-version m))))
      (do
        (io/make-parents (io/file (str down-dir file-name)))
        (io/copy (:body @res) (io/file (str down-dir file-name)))
        (cond
          (= (:driver-type m) :chrome)
            (zip/unzip (str down-dir file-name) down-dir)
          (= (:driver-type m) :firefox)
            (do
              (kio/with-tmps [tar (kio/tmp-file)]
                (gunzip (str down-dir file-name)
                        (.getCanonicalPath tar))
                (untar tar down-dir))))
        (io/delete-file (io/file (str down-dir file-name)))
        (cond
          (= (:driver-type m) :chrome)
          (let [file (io/file (str down-dir) "chromedriver")]
            (.setExecutable file true) file)
          (= (:driver-type m) :firefox)
          (let [file (io/file (str down-dir) "geckodriver")]
            (.setExecutable file true) file))))))

(defn driver-bin [m]
  (let [bin (cond
              (= :firefox (:driver-type m))
              (io/file (str (driver-parent m) "geckodriver"))
              (= :chrome (:driver-type m))
              (io/file (str (driver-parent m) "chromedriver")))]
    (when-not (.exists bin)
      (download-driver m))
    bin))

(defn start-remote-driver [m]
  (let [bin (driver-bin m)
        port (kio/available-port)
        remote-driver
        (if (:xvfb-port m)
          (sh/proc (.getCanonicalPath bin) (str "--port=" port)
                   :env {"DISPLAY" (str ":" (:xvfb-port m))})
          (sh/proc (.getCanonicalPath bin) (str "--port=" port)))]
    (wait-for #(kio/host-port-listening? "localhost" port) 10000 10)
    {:remote-driver remote-driver
     :remote-driver-port port
     :url (str "http://localhost:" port)}))

(defn start-recorded-driver [m]
  (let [bin (driver-bin m)]
    (sh/proc "Xvfb" ":44" "-screen" "0" "1920x1080x24")
    (sh/proc "ffmpeg" "-y" "-f" "x11grab" "-video_size" "1920x1080" "-i" ":44" "-codec:v" "libx264" "-r" "60" "/tmp/glados.mp4")
    (sh/proc (.getCanonicalPath bin) "--port=5555" :env {"DISPLAY" ":44"})))

(defn- chrome-cap [m]
  (let [options (new org.openqa.selenium.chrome.ChromeOptions)
        capabilities (. org.openqa.selenium.remote.DesiredCapabilities chrome)]
    (. options addArguments (concat (if (:driver-args m) (:driver-args m) [])
                                    ["--window-size=1920x1080" "--no-sandbox"]))
    (.setExperimentalOption options "prefs"
      (doto (new java.util.HashMap)
        (.put "profile.default_content_settings.popups" 0)
        (.put "download.default_directory" (mkdownload-dir m))))
    (.setCapability capabilities (. org.openqa.selenium.remote.CapabilityType
                                    ACCEPT_SSL_CERTS) true)
    (.setCapability capabilities (. org.openqa.selenium.remote.CapabilityType
                                    ACCEPT_INSECURE_CERTS) true)
    (.setCapability capabilities (. org.openqa.selenium.chrome.ChromeOptions
                                    CAPABILITY) options)
    capabilities))

(defn create-chrome-driver
  "creates a chromedriver and passes in vector args as command line arguments"
  [m]
  (. System setProperty "wdm.targetPath" ".webdrivers/")
  (. System setProperty "webdriver.chrome.silentOutput", "true")
  (-> (. io.github.bonigarcia.wdm.WebDriverManager chromedriver)
      (.version latest-chrome-version)
      (.setup))
    {:driver (new org.openqa.selenium.chrome.ChromeDriver (chrome-cap m))})

(defn- firefox-cap [m]
  (let [options (new org.openqa.selenium.firefox.FirefoxOptions)
        capabilities (. org.openqa.selenium.remote.DesiredCapabilities firefox)
        profile (new org.openqa.selenium.firefox.FirefoxProfile)]
    (.setPreference profile "browser.download.folderList" 2)
    (.setPreference profile "browser.download.dir" (mkdownload-dir m))
    (.setPreference profile "browser.helperApps.alwaysAsk.force" false)
    (.setPreference profile "browser.helperApps.neverAsk.saveToDisk"
                    (str "text/csv,application/x-msexcel,application/excel,"
                         "application/x-excel,application/vnd.ms-excel,"
                         "image/png,image/jpeg,text/html,text/plain,"
                         "application/msword,application/xml,csv,zip,"
                         "application/x-download,application/zip,"
                         "application/octet-stream"))
    (. options addArguments (if (:driver-args m) (:driver-args m) []))
    (. options setProfile profile)
    (.setCapability
      capabilities
      (. org.openqa.selenium.firefox.FirefoxOptions FIREFOX_OPTIONS) options)
    capabilities))

(defn create-firefox-driver
  "creates a firefoxdriver and passes in vector args as command line arguments"
  [m]
  (. System setProperty "wdm.targetPath" ".webdrivers/")
  (. System setProperty "wdm.geckoDriverVersion" latest-gecko-version)
  (. System setProperty "webdriver.firefox.marionette" "true")
  (. System setProperty "webdriver.firefox.logfile" ".webdrivers/gecko.log")
  (-> (. io.github.bonigarcia.wdm.WebDriverManager firefoxdriver)
      (.version latest-gecko-version)
      (.setup))
    {:driver (new org.openqa.selenium.firefox.FirefoxDriver (firefox-cap m))})

(defn create-remote-driver
  [m]
  (.setLevel (Logger/getLogger "org.openqa.selenium") Level/OFF)
  (let [cap (cond
              (= :chrome (:driver-type m)) (chrome-cap m)
              (= :firefox (:driver-type m)) (firefox-cap m)
              :else (chrome-cap m))]
    {:driver (new org.openqa.selenium.remote.RemoteWebDriver
                  (new java.net.URL (:url m)) cap)}))

(defn set-size [driver width height]
  (.setSize (.window (.manage (:driver driver)))
            (new org.openqa.selenium.Dimension width height)))

(defn headless-remote-driver [m]
  (let [m (merge {:driver-type :chrome} m)
        remote-driver (start-remote-driver m)
        driver (create-remote-driver (dissoc (merge m remote-driver)
                                             :driver-args))]
    (set-size driver 1910 1070)
    (merge driver remote-driver)))
