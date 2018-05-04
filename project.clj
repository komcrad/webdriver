(defproject webdriver "0.3.1"
  :description "A clojure selenium webdriver wrapper"
  :url "https://github.com/komcrad/webdriver"
  :license {:name "LGPL-3.0"
            :url "https://www.gnu.org/licenses/lgpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.seleniumhq.selenium/selenium-java "3.7.1"]
                 [io.github.bonigarcia/webdrivermanager "1.7.2"]
                 [org.slf4j/slf4j-simple "1.7.25"]
                 [komcrad-utils "0.1.5"]]
  :plugins [[com.holychao/parallel-test "0.3.1"]
            [lein-autoreload "0.1.1"]])
