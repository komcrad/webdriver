(ns glados-webdriver.core-test
  (:require [clojure.test :refer :all]
            [glados-webdriver.core :refer :all]
            [clojure.java.io :as io]))
(def test-html-file-url (str "file://" (.getCanonicalPath (io/file "test/resources/index.html"))))

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
      ["--headless"]
      (to driver "https://google.com")
      (is (= "Google Search" (get-element-value driver :name "btnK" :value))))))

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
      ["--headless"]
      (to driver test-html-file-url)
      (is (= 1 (count (get-elements driver :name "p1"))))
      (is (= 2 (count (get-elements driver :id "p2"))))
      (is (= 3 (count (get-elements driver :linkText "paragraph 3"))))
      (is (= 4 (count (get-elements driver :className "p4"))))
      (is (= 5 (count (get-elements driver :xpath "//p[@xpath='p5']"))))
      (is (= 6 (count (get-elements driver :text "paragraph 6"))))
      (is (= 7 (count (get-elements driver :tagName "paragraph")))))))

(deftest ^:parallel get-element-test
  (testing "get-element"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (= "paragraph 1" (.getText (get-element driver :name "p1")))))))

(deftest ^:parallel focused-element-test
  (testing "focused-element")
  (with-all-drivers
    ["--headless"]
    (to driver test-html-file-url)
    (click driver :id "btn1")
    (is (= "Button 1" (get-element-value (focused-element driver) :text)))))

(deftest ^:parallel unfocus-test                                     
    (testing "unfocus")                                                
      (with-all-drivers
        ["--headless"]
        (to driver test-html-file-url)
        (click driver :id "btn1")
        (unfocus driver)
        (is (not (= "Button 1" (get-element-value (focused-element driver) :text))))))

(deftest ^:parallel clear-test
  (testing "clear"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (set-element driver :id "input1" "somestring")
      (clear driver (get-element driver :id "input1"))
      (is (= "" (get-element-value driver :id "input1" :value)))
      (set-element driver :id "input1" "somestring")
      (clear driver :id "input1")
      (is (= "" (get-element-value driver :id "input1" :value))))))

(deftest ^:parallel wait-for-element-test
  (testing "wait-for-element"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (click driver :id "btn2")
      (is (= "potato") (get-element-value (wait-for-element driver :id "input2" 5) :value))
      (click driver :id "btn3")
      (click driver :id "btn2")
      (is (= "potato") (get-element-value (wait-for-element driver :id "input2") :value)))))

(deftest ^:parallel is-visible-test
  (testing "is-visible"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (is-visible driver :id "p2"))
      (is (not (is-visible driver :id "p4315151")))
      (is (is-visible (get-element driver :id "p2")))
      (is (not (is-visible (get-element driver :id "fake news")))))))
