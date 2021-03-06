(defproject webdriver "0.17.1"
  :description "A clojure selenium webdriver wrapper"
  :url "https://github.com/komcrad/webdriver"
  :license {:name "LGPL-3.0"
            :url "https://www.gnu.org/licenses/lgpl-3.0.en.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.seleniumhq.selenium/selenium-java "3.141.59"]
                 [io.github.bonigarcia/webdrivermanager "3.3.0"]
                 [org.slf4j/slf4j-simple "1.7.25"]
                 [me.raynes/fs "1.4.6"]
                 [komcrad-utils "0.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [hiccup "1.0.5"]
                 [http-kit "2.3.0"]
                 [clj-file-zip "0.1.0"]
                 [me.raynes/conch "0.8.0"]
                 [com.google.guava/guava "29.0-jre"]]
  :profiles {:dev
             {:dependencies [[digest "1.4.8"]]}})
