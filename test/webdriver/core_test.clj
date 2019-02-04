(ns webdriver.core-test
  (:require [clojure.test :refer :all]
            [webdriver.core :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [komcrad-utils.io :as kio]))
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
      (driver-quit driver)))
  (testing "headless-insecure-certs"
    (with-all-drivers
      ["--headless"]
      (to driver "https://self-signed.badssl.com/")
      (is (= "self-signed.\nbadssl.com"
             (attr (get-element driver :xpath "//h1") :text))))))

(deftest ^:parallel with-webdriver-test
  (testing "with-webdriver"
    (with-webdriver [d :driver-type :chrome :driver-args ["--headless"]]
      (to d "https://google.com")
      (set-element d :name "q" "silly memes")
      (click (wait-for-element d :xpath "//input[@value = 'Google Search'][1]"))
      (is (s/includes? (attr (first (get-elements d :className "r")) :text)
                                    "50 Hilarious Memes")))
      (with-webdriver [d :driver-type :firefox :driver-args ["--headless"]]
        (to d "https://google.com")
        (set-element d :name "q" "silly memes")
        (click (wait-for-element d :xpath "//input[@value = 'Google Search'][1]"))
        (is (s/includes? (attr (first (get-elements d :className "r")) :text)
                                      "50 Hilarious Memes")))))

(deftest ^:parallel to-test
  (testing "to"
    (with-all-drivers
      ["--headless"]
      (to driver "https://google.com")
      (is (= "Google Search" (get-element-value driver :name "btnK" :value))))))

(deftest ^:parallel driver-quit-test
  (testing "driver-quit"
    (let [driver (create-driver :chrome ["--headless"])]
      (driver-quit driver)
      (is (thrown? org.openqa.selenium.NoSuchSessionException (to driver "https://google.com"))))
    (let [driver (create-driver :firefox ["--headless"])]
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

(deftest siblings-test
  (testing "siblings"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (= ["sib1" "sib2" "sib4" "sib5"]
             (vec (map #(attr % :id)
                       (flatten (vals (siblings driver :id "sib3")))))))
      (is (= "sib5" (attr (first (:following (siblings driver :id "sib4")))
                          :id)))
      (is (= "sib1" (attr (first (:preceding (siblings driver :id "sib2")))
                          :id))))))

(deftest pre-sib-test
  (testing "pre-sib"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (= "sib1" (attr (pre-sib driver :id "sib2") :id)))
      (is (nil? (pre-sib driver :id "sib1"))))))

(deftest post-sib-test
  (testing "pre-sib"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (= "sib5" (attr (post-sib driver :id "sib4") :id)))
      (is (nil? (post-sib driver :id "sib5"))))))

(deftest parent-test
  (testing "parent"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (= "siblings" (attr (parent driver :id "sib4") :id)))
      (is (= "siblings" (attr (parent driver :id "sib3") :id))))))

(deftest select-elm-test
  (testing "select-elm"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (= org.openqa.selenium.support.ui.Select
             (type (select-elm (get-element driver :id "select1")))))
      (is (thrown? org.openqa.selenium.support.ui.UnexpectedTagNameException 
                   (select-elm driver :id "input1"))))))

(deftest select-elm-val-test
  (testing "select-elm-val"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (= "Option 1" (select-elm-val driver :id "select1")))
      (is (= "Option 1" (select-elm-val (get-element driver :id "select1"))))
      (is (= "Option 1" (select-elm-val (select-elm driver :id "select1"))))
      (set-element driver :id "select1" "Option 2")
      (is (= "Option 2" (select-elm-val driver :id "select1")))
      (is (= "Option 2" (select-elm-val (get-element driver :id "select1"))))
      (is (= "Option 2" (select-elm-val (select-elm driver :id "select1")))))))

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

(deftest ^:parallel scroll-into-view-test
  (testing "scroll-into-view"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (let [elm (get-element driver :id "btn1")]
        (scroll-into-view driver elm)
        (is (nil? (.click elm)))
        (click driver :name "span1")
        (scroll-into-view driver :id "btn1")
        (is (nil? (.click elm)))))))

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
      (is (= "potato") (get-element-value (wait-for-element driver :id "input2") :value))
      (is (thrown? Exception
                   (wait-for-element driver :id "notanelement" 0 500)))
      (let [start-time (System/currentTimeMillis)]
        (is (thrown? Exception
                     (wait-for-element driver :id "notanelement" 1 1200)))
        (is (< 1200 (- (System/currentTimeMillis) start-time)))))))

(deftest ^:parallel wait-elm-dom-test
  (testing "wait-elm-dom"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (click driver :id "btn2")
      (is (= "potato" (attr (wait-elm-dom driver :id "input2") :value)))
      (click driver :id "btn5")
      (is (thrown? Exception (wait-elm-dom driver :id "input7" 1)))
      (is (= "are you still there?" (attr (wait-elm-dom driver :id "input7") :value)))
      (is (thrown? Exception (wait-elm-dom driver :id "notanelement" 0))))))

(deftest enabled?-test
  (testing "enabled?"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (not (enabled? driver :id "disabledbtn")))
      (is (not (enabled? (get-element driver :id "disabledbtn"))))
      (is (enabled? driver :id "btn5"))
      (is (enabled? (get-element driver :id "btn5"))))))

(deftest ^:parallel is-visible-test
  (testing "is-visible"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (is-visible driver :id "p2"))
      (is (not (is-visible driver :id "p4315151")))
      (is (is-visible (get-element driver :id "p2")))
      (is (not (is-visible (get-element driver :id "fake news"))))
      (is (not (is-visible driver :id "disabledbtn")))
      (is (not (is-visible (get-element driver :id "disabledbtn")))))))

(deftest ^:parallel visible?-test
  (testing "visible?"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (is (visible? driver :id "p2"))
      (is (visible? (get-element driver :id "p2")))
      (is (not (visible? driver :id "p2fakeelm")))
      (is (not (visible? (get-element driver :id "p2fakeelm"))))
      (is (visible? driver :id "disabledbtn"))
      (is (visible? (get-element driver :id "disabledbtn"))))))

(deftest ^:parallel input-text-test
  (testing "input-text"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (input-text driver (get-element driver :id "input1") "hello there" true)
      (is (= "hello there" (get-element-value driver :id "input1" :value)))
      (input-text driver (get-element driver :id "input1") "world" false)
      (is (= "hello thereworld" (get-element-value driver :id "input1" :value))))))

(deftest ^:parallel set-file-input-test
  (testing "set-file-input"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (kio/with-tf [temp]
        (let [temp-path (.getCanonicalPath temp)
              temp-name (.getName temp)]
        (set-file-input (get-element driver :id "file1") temp-path)
        (set-file-input (get-element driver :id "file2") temp-path)
        (is (s/includes? (attr driver :id "file1" :value) temp-name))
        (is (s/includes? (attr driver :id "file2" :value) temp-name))
        (is (s/includes? (attr driver :id "file2" :style) "none;")))))))

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
(deftest set-elms-test
  (testing "set-elms"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (set-elms driver :id [:input3 "hello" :input4 "there" :input5 "world"])
      (are [elm val] (= (attr (get-element driver :id (name elm)) :value) val)
           :input3 "hello" :input4 "there" :input5 "world"))))

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

(deftest options-test
  (testing "options"
    (with-all-drivers ["--headless"]
      (to driver "https://google.com")
      (insert-html driver (get-element driver :id "viewport")
                   (html [:select {:id "options"} [:options [:option "option 1"]
                                                   [:option "option 2"]]]))
      (is (= ["option 1" "option 2"] (options driver :id "options")))
      (is (nil? (options driver :id "viewport"))))))

(deftest ^:parallel css-test
  (testing "css"
    (with-all-drivers
      ["--headless"]
      (to driver test-html-file-url)
      (let [elm (get-element driver :id "footer")]
        (is (= "100" (css elm "z-index")))
        (is (= "0px" (css elm "left")))
        (is (= "fixed" (css elm "position"))))
      (is (= "100" (css driver :id "footer" "z-index")))
      (is (= "0px" (css driver :id "footer" "left")))
      (is (= "fixed" (css driver :id "footer" "position"))))))

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
      (scroll-into-view driver :id "btn4")
      (click (get-element driver :id "btn4"))
      (is (= "toothpick" (get-element-value driver :id "input6" :value)))
      (click driver :id "btn4" "btn4" "btn4")
      (is (= (count (get-elements driver :id "input6")) 4)))))

(deftest ^:parallel wait-click-test
  (testing "wait-click"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (let [time1 (Float/parseFloat (nth (s/split
                                           (with-out-str
                                             (time (try (wait-click driver :name "fakelement")
                                                        (catch Exception e)))) #" ") 2))
            time2 (Float/parseFloat (nth (s/split
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

(deftest try-click-test
  (testing "try-click"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (click driver :id "btn2")
      (is (try-click driver :id "input2" 10))
      (to driver test-html-file-url)
      (click driver :id "btn2")
      (is (not (try-click driver :id "input2" 2)))
      (to driver test-html-file-url)
      (click driver :id "btn2")
      (is (try-click driver :id "input2")))))

(deftest wait-q-test
  (testing "wait-q"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (is (nil? (wait-q driver :id "input2" 2)))
      (to driver test-html-file-url)
      (click driver :id "btn2")
      (is (= org.openqa.selenium.remote.RemoteWebElement
             (type (wait-q driver :id "input2" 10))))
      (click driver :id "btn2")
      (Thread/sleep 6000)
      (is (= 2 (count (wait-q driver :id "input2" 10))))
      (execute-script driver (str "var tmp = document.createElement('input');"
                                  "tmp.setAttribute('id', 'input2');"
                                  "tmp.setAttribute('hidden', 'true');"
                                  "document.body.appendChild(tmp);"))
      (is (= 3 (count (wait-q driver :id "input2" 10))))
      (is (= 2 (count (wait-q driver :id "input2" 10 true))))
      (to driver test-html-file-url)
      (click driver :id "btn2")
      (is (= org.openqa.selenium.remote.RemoteWebElement
             (type (wait-q driver :id "input2")))))))

(deftest ^:parallel alert-text-test
  (testing "alert-text"
    (with-all-drivers ["--headless"]
      (execute-script driver "alert('hello world')")
      (is (= "hello world" (alert-text driver))))))

(deftest ^:parallel alert-accept-test
  (testing "alert-accept"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (execute-script driver
        (str "var node = document.createElement('span');"
             "var textnode = document.createTextNode(alert('hi'));"
             "node.appendChild(textnode);"
             "node.setAttribute('id', 'alert-result');"
             "document.getElementsByTagName('body')[0]"
             ".appendChild(node);"))
      (is (= "hi" (alert-text driver)))
      (alert-accept driver)
      (is (= "undefined" (attr driver :id "alert-result" :text))))))

(deftest ^:parallel alert-dismiss-test
  (testing "alert-dismiss"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (execute-script driver
        (str "var node = document.createElement('span');"
             "var textnode = document.createTextNode(confirm('hi'));"
             "node.appendChild(textnode);"
             "node.setAttribute('id', 'alert-result');"
             "document.getElementsByTagName('body')[0]"
             ".appendChild(node);"))
      (is (= "hi" (alert-text driver)))
      (alert-accept driver)
      (is (= "true" (attr driver :id "alert-result" :text)))
      (execute-script driver
        (str "var node = document.createElement('span');"
             "var textnode = document.createTextNode(confirm('hey'));"
             "node.appendChild(textnode);"
             "node.setAttribute('id', 'alert-result2');"
             "document.getElementsByTagName('body')[0]"
             ".appendChild(node);"))
      (is (= "hey" (alert-text driver)))
      (alert-dismiss driver)
      (is (= "false" (attr driver :id "alert-result2" :text))))))

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

(deftest insert-html-test
  (testing "insert-html"
    (with-all-drivers ["--headless"]
      (to driver "https://google.com")
      (insert-html driver (get-element driver :id "viewport")
                   (html [:h1 {:id "bobloblaw"} "bobloblaw"]))
      (is (= "bobloblaw" (attr driver :id "bobloblaw" :text)))
      (is (= "bobloblaw" (attr driver :xpath
                               "//div[@id='viewport']/h1[@id='bobloblaw']" :text))))))

(deftest delete-elm-test
  (testing "delete-elm"
    (with-all-drivers ["--headless"]
      (to driver "https://google.com")
      (insert-html driver (get-element driver :id "viewport")
                   (html [:h1 {:id "bobloblaw"} "bobloblaw"]))
      (is (= "bobloblaw" (attr driver :id "bobloblaw" :text)))
      (delete-elm driver (get-element driver :id "bobloblaw"))
      (is (nil? (get-element driver :id "bobloblaw")))
      (insert-html driver (get-element driver :id "viewport")
                   (html [:h1 {:id "bobloblaw"} "bobloblaw"]))
      (is (= "bobloblaw" (attr driver :id "bobloblaw" :text)))
      (delete-elm driver :id "bobloblaw")
      (is (nil? (get-element driver :id "bobloblaw"))))))

(deftest wait-for-trans-test
  (testing "wait-for-trans"
    (with-all-drivers ["--headless"]
      (to driver test-html-file-url)
      (click driver :id "transition")
      (is (not (= "200px" (css driver :id "transition" "width"))))
      (Thread/sleep 1000)
      (let [now (System/currentTimeMillis)]
        (wait-for-trans driver (get-element driver :id "transition")
                                 #(click driver :id "transition"))
        (is (= "300px" (css driver :id "transition" "width")))
        (is (> 2000 (- (System/currentTimeMillis) now)))))))
