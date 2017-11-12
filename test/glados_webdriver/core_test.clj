(ns glados-webdriver.core-test
  (:require [clojure.test :refer :all]
            [glados-webdriver.core :refer :all]
            [clojure.java.io :as io]))
(def test-html-file-url (str "file://" (.getCanonicalPath (io/file "test/resources/index.html"))))
(defn with-all-drivers
  "expects a function that excepts driver as an argument"
  [f]
  (let [driver (create-driver :chrome ["--headless"])]
    (f driver)
    (driver-quit driver))
  (let [driver (create-driver :firefox ["--headless"])]
    (f driver)
    (driver-quit driver)))

(deftest ^:parallel create-driver-test
  (testing "create-driver"
    (let [driver (create-driver :chrome [])]
      (is (= "class org.openqa.selenium.chrome.ChromeDriver" (.toString (type driver))))
      (to driver (str "file://" (.getCanonicalPath (io/file "test/resources/index.html"))))
      (driver-quit driver))
    (let [driver (create-driver :chrome ["--headless"])]
      (is (= "class org.openqa.selenium.chrome.ChromeDriver" (.toString (type driver))))
      (driver-quit driver))
    (let [driver (create-driver :firefox [])]
      (is (= "class org.openqa.selenium.firefox.FirefoxDriver" (.toString (type driver))))
      (driver-quit driver))
    (let [driver (create-driver :firefox ["--headless"])]
      (is (= "class org.openqa.selenium.firefox.FirefoxDriver" (.toString (type driver))))
      (driver-quit driver))))

(deftest ^:parallel to-test
  (testing "to"
    (with-all-drivers
      (fn [driver]
        (to driver "https://google.com")
        (is (= "Google Search" (get-element-value driver :name "btnK" :value)))))))

(deftest ^:parallel driver-quit-test
  (testing "driver-quit"
    (let [driver (create-driver :chrome)]
      (driver-quit driver)
      (is (thrown? org.openqa.selenium.NoSuchSessionException (to driver "https://google.com"))))
    (let [driver (create-driver :firefox)]
      (driver-quit driver)
      (is (thrown? org.openqa.selenium.NoSuchSessionException (to driver "https://google.com"))))))

(deftest ^:parallel get-elements-test
  (testing "get-elements"
    (with-all-drivers
      (fn [driver]
        (to driver test-html-file-url)
        (is (= 1 (count (get-elements driver :name "p1"))))
        (is (= 2 (count (get-elements driver :id "p2"))))
        (is (= 3 (count (get-elements driver :linkText "paragraph 3"))))
        (is (= 4 (count (get-elements driver :className "p4"))))
        (is (= 5 (count (get-elements driver :xpath "//p[@xpath='p5']"))))
        (is (= 6 (count (get-elements driver :text "paragraph 6"))))
        (is (= 7 (count (get-elements driver :tagName "paragraph"))))))))

(deftest ^:parallel get-element-test
  (testing "get-element"
    (with-all-drivers
      (fn [driver]
        (to driver test-html-file-url)
        (is (= "paragraph 1" (.getText (get-element driver :name "p1"))))))))

(deftest ^:parallel focused-element-test
  (testing "focused-element")
  (with-all-drivers
    (fn [driver]
      (to driver test-html-file-url)
      (click driver :id "btn1")
      (is (= "Button 1" (get-element-value driver (focused-element driver) :text))))))
