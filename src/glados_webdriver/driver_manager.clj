(ns glados-webdriver.driver-manager
  (:gen-class))

(defn create-chrome-driver     
  "creates a chromedriver and passes in vector args as command line arguments"
  [args]                       
  (. System setProperty "wdm.targetPath" ".webdrivers/")
  (. (. io.github.bonigarcia.wdm.ChromeDriverManager getInstance) setup)
  (if (empty? args)        
    (new org.openqa.selenium.chrome.ChromeDriver)
    (do                    
      (let [options (new org.openqa.selenium.chrome.ChromeOptions)
            capabilities (. org.openqa.selenium.remote.DesiredCapabilities chrome)]
        (. options addArguments args)
        (map #(println %) args)
        (.setCapability capabilities (. org.openqa.selenium.remote.CapabilityType ACCEPT_SSL_CERTS) true)
        (.setCapability capabilities (. org.openqa.selenium.chrome.ChromeOptions CAPABILITY) options)
        (new org.openqa.selenium.chrome.ChromeDriver capabilities)))))

(defn create-firefox-driver
  "creates a firefoxdriver and passes in vector args as command line arguments"
  [args]
  (. System setProperty "wdm.targetPath" ".webdrivers/")
  (. System setProperty "wdm.geckoDriverVersion" "0.19.1")
  (. System setProperty "webdriver.firefox.marionette" "true")
  (. System setProperty "webdriver.firefox.logfile" ".webdrivers/gecko.log")
  (. (. io.github.bonigarcia.wdm.FirefoxDriverManager getInstance) setup)
  (if (empty? args)
    (new org.openqa.selenium.firefox.FirefoxDriver)
    (do
      (let [options (new org.openqa.selenium.firefox.FirefoxOptions)
            capabilities (. org.openqa.selenium.remote.DesiredCapabilities firefox)]
        (. options addArguments args)
        (.setCapability capabilities (. org.openqa.selenium.firefox.FirefoxOptions FIREFOX_OPTIONS) options)
        (new org.openqa.selenium.firefox.FirefoxDriver capabilities)))))
