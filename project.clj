(defproject glados-webdriver "0.1.0-SNAPSHOT"
  :description "A clojure selenium webdriver wrapper"
  :url "https://github.com/komcrad/glados-webdriver"
  :license {:name "GPL-2.0"
            :url "https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.seleniumhq.selenium/selenium-java "3.7.1"]
                 [io.github.bonigarcia/webdrivermanager "1.7.2"]
                 [org.slf4j/slf4j-simple "1.7.25"]]
  :plugins [[com.holychao/parallel-test "0.3.1"]
            [lein-autoreload "0.1.1"]])
