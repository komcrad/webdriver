(ns webdriver.driver-manager
  (:gen-class))

(defn- mkdownload-dir [m]
  (let [dir (:download-dir m)]
    (if dir
      (do (.mkdir (clojure.java.io/file dir))
          (if (not (.isDirectory (clojure.java.io/file dir)))
            (throw (Exception. ":download-dir must be a folder."))
            (.getCanonicalPath (clojure.java.io/file dir))))
      (System/getProperty "user.dir"))))

(defn create-chrome-driver
  "creates a chromedriver and passes in vector args as command line arguments"
  [m]
  (. System setProperty "wdm.targetPath" ".webdrivers/")
  (. System setProperty "webdriver.chrome.silentOutput", "true")
  (-> (. io.github.bonigarcia.wdm.WebDriverManager chromedriver)
      (.version "73.0.3683.68")
      (.setup))
  (let [options (new org.openqa.selenium.chrome.ChromeOptions)
        capabilities (. org.openqa.selenium.remote.DesiredCapabilities chrome)]
    (. options addArguments (concat (if (:driver-args m) (:driver-args m) [])
                                    ["--window-size=1920x1080"]))
    (.setExperimentalOption options "prefs"
                            (doto (new java.util.HashMap)
                             (.put "profile.default_content_settings.popups" 0)
                             (.put "download.default_directory"
                                   (mkdownload-dir m))))
    (.setCapability capabilities (. org.openqa.selenium.remote.CapabilityType ACCEPT_SSL_CERTS) true)
    (.setCapability capabilities (. org.openqa.selenium.remote.CapabilityType ACCEPT_INSECURE_CERTS) true)
    (.setCapability capabilities (. org.openqa.selenium.chrome.ChromeOptions CAPABILITY) options)
    (new org.openqa.selenium.chrome.ChromeDriver capabilities)))

(defn create-firefox-driver
  "creates a firefoxdriver and passes in vector args as command line arguments"
  [m]
  (. System setProperty "wdm.targetPath" ".webdrivers/")
  (. System setProperty "wdm.geckoDriverVersion" "0.24.0")
  (. System setProperty "webdriver.firefox.marionette" "true")
  (. System setProperty "webdriver.firefox.logfile" ".webdrivers/gecko.log")
  (-> (. io.github.bonigarcia.wdm.WebDriverManager firefoxdriver)
      (.version "0.24.0")
      (.setup))
  (let [options (new org.openqa.selenium.firefox.FirefoxOptions)
        capabilities (. org.openqa.selenium.remote.DesiredCapabilities firefox)
        profile (new org.openqa.selenium.firefox.FirefoxProfile)]
    (.setPreference profile "browser.download.folderList" 2)
    (.setPreference profile "browser.download.dir" (mkdownload-dir m))
    (.setPreference profile "browser.helperApps.alwaysAsk.force" false)
    (.setPreference profile "browser.helperApps.neverAsk.saveToDisk"
                    "text/csv,application/x-msexcel,application/excel,application/x-excel,application/vnd.ms-excel,image/png,image/jpeg,text/html,text/plain,application/msword,application/xml,csv,zip,application/x-download,application/zip")
    (. options addArguments (if (:driver-args m) (:driver-args m) []))
    (. options setProfile profile)
    (.setCapability capabilities (. org.openqa.selenium.firefox.FirefoxOptions FIREFOX_OPTIONS) options)
    (new org.openqa.selenium.firefox.FirefoxDriver capabilities)))
