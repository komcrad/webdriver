(ns webdriver.core-test
  (:require [clojure.test :refer :all]
            [webdriver.core :refer :all]
            [clojure.java.io :as io]))
(def test-html-file-url (str "file://" (.getCanonicalPath (io/file "test/resources/index.html"))))

(deftest ^:parallel create-driver-test
  (testing "create-driver"
    (let [driver (create-driver :chrome ["--headless"])]
      (is (= "class org.openqa.selenium.chrome.ChromeDriver" (.toString (type driver))))
      (to driver (str "file://" (.getCanonicalPath (io/file "test/resources/index.html"))))
      (driver-quit driver))
    (let [driver (create-driver :chrome ["--headless"])]
      (is (= "class org.openqa.selenium.chrome.ChromeDriver" (.toString (type driver))))
      (driver-quit driver))
    (let [driver (create-driver :firefox ["--headless"])]
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

(deftest ^:parallel get-visible-element-test
  (testing "get-visible-element"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (= "not really hidden button" (.getText (get-visible-element driver :id "hiddenbutton")))))))

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

(deftest ^:parallel implicit-wait-test
  (testing "implicit-wait")
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (click driver :id "btn2")
      (is (thrown? NullPointerException (get-element-value driver :id "input2" :value)))
      (to driver test-html-file-url)
      (implicit-wait driver 10)
      (click driver :id "btn2")
      (is (= "potato" (get-element-value driver :id "input2" :value)))))

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

(deftest ^:parallel input-text-test
  (testing "input-text"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (input-text driver (get-element driver :id "input1") "hello there" true)
      (is (= "hello there" (get-element-value driver :id "input1" :value)))
      (input-text driver (get-element driver :id "input1") "world" false)
      (is (= "hello thereworld" (get-element-value driver :id "input1" :value))))))

(deftest ^:parallel set-element-test
  (testing "set-element"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (set-element driver :id "input1" "hello there")
      (is (= "hello there" (get-element-value driver :id "input1" :value)))
      (set-element driver (get-element driver :id "input1") "hello world")
      (is (= "hello world" (get-element-value driver :id "input1" :value)))
      (set-element driver :id "select1" "Option 2")
      (is (= "Option 2" (get-element-value driver :id "select1" :value))))))

(deftest ^:parallel set-elements-test
  (testing "set-elements"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (set-elements
        driver
        [(get-element driver :id "input1") (get-element driver :id "select1")]
        ["hello there world" "Option 2"])
      (is (= "hello there world" (get-element-value driver :id "input1" :value)))
      (is (= "Option 2" (get-element-value driver :id "select1" :value)))
      (set-elements driver :id
                    ["input1" "input3" "input4" "input5"]
                    ["input1val" "input3val" "input4val" "input5val"])
      (is (= "input1val" (get-element-value driver :id "input1" :value)))
      (is (= "input3val" (get-element-value driver :id "input3" :value)))
      (is (= "input4val" (get-element-value driver :id "input4" :value)))
      (is (= "input5val" (get-element-value driver :id "input5" :value))))))

(deftest ^:parallel attr-test
  (testing "attr"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (let [elm (get-element driver :name "span1")]
        (is (= "span1" (attr elm :name)))
        (is (= "span1" (attr driver :name "span1" :name)))
        (is (= "span1id" (attr elm :id)))
        (is (= "span1id" (attr driver :name "span1" :id)))
        (is (= "hello" (attr elm :onclick)))
        (is (= "hello" (attr driver :name "span1" :onclick)))
        (is (= "im fake" (attr elm :fakeattribute)))
        (is (= "im fake" (attr driver :name "span1" :fakeattribute)))
        (is (= "span 1" (attr elm :text)))
        (is (= "span 1" (attr driver :name "span1" :text)))))))

(deftest ^:parallel get-element-value-test
  (testing "get-element-value"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (set-element driver :id "input1" "hello")
      (set-element driver :id "select1" "Option 3")
      (is (= "hello" (get-element-value driver :id "input1" :value)))
      (is (= "Option 3" (get-element-value driver :id "select1" :value))))))

(deftest ^:parallel click-test
  (testing "click"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (click driver :id "btn4")
      (is (= "toothpick" (get-element-value driver :id "input6" :value)))
      (to driver "https://google.com")
      (to driver test-html-file-url)
      (click (get-element driver :id "btn4"))
      (is (= "toothpick" (get-element-value driver :id "input6" :value)))
      (click driver :id "btn4" "btn4" "btn4")
      (is (= (count (get-elements driver :id "input6")) 4)))))

(deftest ^:parallel wait-click-test
  (testing "wait-click"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (let [time1 (Float/parseFloat (nth (clojure.string/split
                                           (with-out-str
                                             (time (try (wait-click driver :name "fakelement")
                                                        (catch Exception e)))) #" ") 2))
            time2 (Float/parseFloat (nth (clojure.string/split
                                           (with-out-str
                                             (time (try (wait-click driver :name "fakelement" 5)
                                                        (catch Exception e)))) #" ") 2))]
        (is (< 10000 time1))
        (is (> 11000 time1))
        (is (<  5000 time2))
        (is (>  6000 time2)))
      (click driver :id "btn2")
      (is (thrown? Exception (wait-click driver :id "input2" 1)))
      (is (nil? (wait-click driver :id "input2"))))))

(deftest ^:parallel alert-text-test
  (testing "alert-text"
    (with-all-drivers []
      (execute-script driver "alert('hello world')")
      (is (= "hello world" (alert-text driver))))))

(deftest ^:parallel alert-accept-test
  (testing "alert-accept"
    (with-all-drivers []
      (to driver test-html-file-url)
      (execute-script driver "alert(alert('hello world'))")
      (is (= "hello world" (alert-text driver)))
      (is (thrown? Exception (click driver :id "btn1")))
      (alert-accept driver)
      (is (= "undefined" (alert-text driver)))
      (alert-accept driver)
      (is (= nil (click driver :id "btn1"))))))

(deftest ^:parallel alert-dismiss-test
  (testing "alert-dismiss"
    (with-all-drivers []
      (to driver test-html-file-url)
      (execute-script driver "alert(confirm('do you like jolly bears?'))")
      (is (= "do you like jolly bears?" (alert-text driver)))
      (alert-accept driver)
      (is (= "true" (alert-text driver)))
      (alert-accept driver)
      (execute-script driver "alert(confirm('do you like jolly bears?'))")
      (is (= "do you like jolly bears?" (alert-text driver)))
      (alert-dismiss driver)
      (is (= "false" (alert-text driver)))
      (alert-accept driver))))

(deftest ^:parallel iframe-test
  (testing "iframe"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (is (thrown? java.lang.NullPointerException (get-element-value driver :id "iframeBChild" :text)))
      (iframe driver 1)
      (is (= "I'm child B" (get-element-value driver :id "iframeBChild" :text)))
      (to driver test-html-file-url)
      (iframe driver :id "iframeA")
      (is (= "I'm child A" (get-element-value driver :id "iframeAChild" :text))))))

(deftest ^:parallel iframe-parent-test
  (testing "iframe"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (iframe driver :id "iframeB")
      (iframe driver :id "iframeC")
      (is (= "I'm child C" (get-element-value driver :id "iframeCChild" :text)))
      (iframe-parent driver)
      (is (= "I'm child B" (get-element-value driver :id "iframeBChild" :text))))))

(deftest ^:parallel iframe-default-test
  (testing "iframe-default"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (iframe driver :id "iframeB")
      (iframe driver :id "iframeC")
      (is (= "I'm child C" (get-element-value driver :id "iframeCChild" :text)))
      (iframe-default driver)
      (is (= "paragraph 1" (get-element-value driver :name "p1" :text))))))

(deftest ^:parallel cookie-test
  (testing "cookie"
    (with-all-drivers ["--headless"]
      (to driver "https://google.com")
      (cookie driver "sillynonsensecookie" "I'm silly")
      (is (= "I'm silly" (cookie driver "sillynonsensecookie")))
      (cookie driver "sillynonsensecookie" "I'm silly nonsense")
      (is (= "I'm silly nonsense" (cookie driver "sillynonsensecookie"))))))
