(ns webdriver.core
  (:gen-class)
  (:import
    [java.util.concurrent TimeUnit]
    [org.openqa.selenium.remote RemoteWebDriver]
    [org.openqa.selenium WebElement]
    [org.openqa.selenium.support.ui WebDriverWait ExpectedConditions])
  (:require [webdriver.driver-manager :as dm]
            [komcrad-utils.wait :refer [wait-for]]
            [clojure.java.io :as io]))

(defn create-driver
  "creates a chrome or firefox driver based on passing in :chrome or :firefox.
   args would be a verctor for command line arguments like [\"--headless\"]"
  ([driver-type args]
  (. (. (. java.util.logging.LogManager getLogManager) getLogger "") setLevel (java.util.logging.Level/OFF))
  (cond
    (= :chrome driver-type)
      (dm/create-chrome-driver args)
    (= :firefox driver-type)
      (dm/create-firefox-driver args)
    :else (throw (Exception. (str "driver-type " driver-type " is unsupported.\n"
                                  "Supported values:\n"
                                  ":chrome, :firefox")))))
  ([driver-type]
   (create-driver driver-type ["--headless"])))


(defmacro with-driver
  "creates a driver, executes the forms in body, and closes the driver.
   driver is closed in the case of an exception"
  ([driver-type driver-args & body]
  (list 'let (vector 'driver (list 'create-driver driver-type driver-args))
        (list 'try (cons 'do body) '(catch Exception e (throw e)) '(finally (driver-quit driver))))))

(defmacro with-webdriver
  [[driver & {:keys [driver-type driver-args]
              :as params
              :or {driver-args []
                   driver-type :chrome}}] & body]
  `(let [driver-type# ~driver-type
         driver-args# ~driver-args
         ~driver (webdriver.core/create-driver driver-type# driver-args#)]
    (try
      ~@body
      (catch Exception e# (throw e#))
      (finally (webdriver.core/driver-quit ~driver)))))

(defmacro with-all-drivers
  "Same as with-driver but evaluates the forms in body agains all supported driver types.
   See examples in the unit tests"
  [driver-args & body]
  `(do (with-driver :chrome ~driver-args ~@body)
       (with-driver :firefox ~driver-args ~@body)))

(defn to
  "Navigates driver to the given url"
  ([driver url] (. driver get url)))

(defn to-localhost
  "Navigate driver to localhost, ignoring thrown exceptions"
  [driver]
  (try (to driver "http://localhost") (catch Exception e)))

(defn driver-quit
  "calls quit on driver"
  ([driver]
    (. driver quit)))

(defn by
  "Returns a By object. Used for element queries"
  [lookup-type lookup-string]
  (cond
    (= :id lookup-type)
      (. org.openqa.selenium.By id lookup-string)
    (= :name lookup-type)
      (. org.openqa.selenium.By name lookup-string)
    (= :linkText lookup-type)
      (. org.openqa.selenium.By linkText lookup-string)
    (= :className lookup-type)
      (. org.openqa.selenium.By className lookup-string)
    (= :xpath lookup-type)
      (. org.openqa.selenium.By xpath lookup-string)
    (= :text lookup-type)
      (. org.openqa.selenium.By xpath
       (str "//*[contains(text(), '" lookup-string "')]"))
    (= :tagName lookup-type)
      (. org.openqa.selenium.By tagName lookup-string)
    :else (throw (Exception. (str "get-element has no option \"" lookup-type "\"")))))

(defn get-elements
  "finds elements that match lookup-type and lookup-string and returns a vector of those WebElements"
  [driver lookup-type lookup-string]
  (if lookup-type
    (. driver findElements (by lookup-type lookup-string))))

(defn get-element
  "returns the first element matching lookup-type and lookup-string"
  ([driver lookup-type lookup-string]
  (try (nth (get-elements driver lookup-type lookup-string) 0)
       (catch Exception e nil))))

(defn get-visible-element
  "returns the first visible elmeent matching lookip-type and lookup-string"
  ([driver lookup-type lookup-string]
   (try (loop [elements (get-elements driver lookup-type lookup-string)]
          (if (not (empty? elements))
           (if (.isDisplayed (first elements))
             (first elements)
             (recur (rest elements))) nil))
        (catch Exception e nil))))

(defn q
  "Finds and returns webelement with name, id, tagName, className, linkText, text, or xpath.
   Note: not great to use if using implicit waits."
  [driver s]
   (loop [types [:name :id :tagName
                 :className :linkText :text
                 :xpath]]
     (if-let
       [element
       (try
         (get-element driver (first types) s)
         (catch Exception e
           (if (> 2 (count types))
             (throw (Exception. (str "Could not find element "
                                    "with name/linkText/tagName/id of "
                                    s))))))]
         element
         (recur (drop 1 types)))))

(defn focused-element
  [driver]
  "Returns the current focused webelement"
  (.activeElement (.switchTo driver)))

(defn send-keys
  "sends element the keys found in str s"
  [element s]
  (.sendKeys element (into-array CharSequence [s])))

(defn execute-script
    "Executes js in webdriver"
      [^RemoteWebDriver webdriver js & js-args]
        (.executeScript webdriver ^String js (into-array Object js-args)))

(defn unfocus
  "unfocuses all elements"
  [driver]
  (execute-script driver (str "var tmp = document.createElement('input');"
                              "document.body.appendChild(tmp); tmp.focus();"
                              "document.body.removeChild(tmp);")))

(defn scroll-into-view
  "scrolls webelemnt into view"
  ([driver webelement]
   (execute-script driver "arguments[0].scrollIntoView({block: 'center'});" webelement))
  ([driver lookup-type lookup-string]
   (scroll-into-view driver (get-element driver lookup-type lookup-string))))

(defn clear
  "clears webelement"
  ([driver webelement]
  (scroll-into-view driver webelement)
  (.sendKeys webelement
    (into-array CharSequence
                [(org.openqa.selenium.Keys/chord
                 (into-array CharSequence [(. org.openqa.selenium.Keys CONTROL) "a"]))]))
  (.sendKeys webelement
    (into-array CharSequence
    [(. org.openqa.selenium.Keys BACK_SPACE)])))

  ([driver lookup-type lookup-string]
  (clear driver (get-visible-element driver lookup-type lookup-string))))

(defn implicit-wait
  "sets driver's implicit wait timeout (in seconds)"
  [driver timeout]
  (.implicitlyWait (.timeouts (.manage driver)) timeout (java.util.concurrent.TimeUnit/SECONDS)))

(defn wait-for-element
  "Explicitly waits for element to be clickable with a timeout of max-wait (seconds)
   and a poll-interval (milliseconds)"
  ([driver lookup-type lookup-string max-wait poll-interval]
   (-> (new WebDriverWait driver max-wait)
       (.pollingEvery poll-interval (TimeUnit/MILLISECONDS))
       (.until (. ExpectedConditions elementToBeClickable (by lookup-type lookup-string)))))
  ([driver lookup-type lookup-string max-wait]
   (wait-for-element driver lookup-type lookup-string max-wait 500))
  ([driver lookup-type lookup-string]
   (wait-for-element driver lookup-type lookup-string 10)))

(defn wait-elm-dom
  "Waits for element to exist in dom with a timeout of max-wait (seconds)"
  ([driver lookup-type lookup-string max-wait]
   (. (new org.openqa.selenium.support.ui.WebDriverWait driver max-wait) until
      (. org.openqa.selenium.support.ui.ExpectedConditions presenceOfElementLocated (by lookup-type lookup-string))))
  ([driver lookup-type lookup-string]
   (wait-elm-dom driver lookup-type lookup-string 10)))

(defn is-visible
  "Returns true if element is visible and enabled"
  ([element]
  (try (and (.isEnabled element) (.isDisplayed element))
       (catch Exception e false)))

  ([driver lookup-type lookup-string]
  (try (is-visible (get-element driver lookup-type lookup-string))
       (catch Exception e false))))

(defn visible?
  "Returns true if element is visible"
  ([elm]
   (try (.isDisplayed elm)
        (catch Exception e false)))
  ([driver lookup-type lookup-string]
   (try (visible? (get-element driver lookup-type lookup-string))
        (catch Exception e false))))

(defn input-text
  "sets the value of a text input. If clear-element, element will be cleared before
  setting the text input"
  [driver webelement s clear-element]
    (scroll-into-view driver webelement)
    (if (= "class java.lang.String" (.toString (type webelement)))
      (input-text driver (get-element driver webelement) s clear-element)
      (do (if clear-element
            (clear driver webelement))
          (.sendKeys webelement (into-array CharSequence [s]))
          (.sendKeys webelement (into-array CharSequence
                                            [(. org.openqa.selenium.Keys CONTROL)]))
          (unfocus driver)
          webelement)))

(defn set-file-input
  "Sets file input's path"
  [element s]
  (if (not (visible? element))
    (let [driver (.getWrappedDriver element)]
        (execute-script driver "arguments[0].style.display = 'block';" element)
        (wait-for #(visible? element) 1000 50)
        (send-keys element s)
        (execute-script driver "arguments[0].style.display = 'none';" element))
    (send-keys element s)))

(defn set-element
  "sets element e to value s. For select or input elements"
  ([driver e s]
  (scroll-into-view driver e)
  (if (= "select" (.getTagName e))
   (do
     (. (new org.openqa.selenium.support.ui.Select e)
        selectByVisibleText s)
     e)
   (do
     (clear driver e)
     (.sendKeys e (into-array CharSequence [s]))
     (.sendKeys e (into-array CharSequence [(. org.openqa.selenium.Keys CONTROL)]))
     e)))
  ([driver lookup-type lookup-string s]
  (set-element driver (get-visible-element driver lookup-type lookup-string) s)))

(defn set-elements
  "sets coll of elements e to coll of values v"
  ([driver e v]
  (if (= (count e) (count v))
    (do
      (loop [elements e values v]
        (if (> (count elements) 0)
         (do
           (set-element driver (first elements) (first values))
           (recur (rest elements) (rest values))))))))
  ([driver lookup-type lookup-strings values]
   (when (= (count lookup-strings) (count values))
     (loop [lookup-strings lookup-strings values values]
       (when (not (empty? lookup-strings))
         (set-element driver lookup-type (first lookup-strings) (first values))
         (recur (rest lookup-strings) (rest values)))))))

(defn set-elms
  "partitions coll into lists of 2
   each list becomes a key value pair where key is an element identifier
   and value is the value that element will be set to
   Eg: (set-elms driver :id [:input1 \"hello\" :input2 \"world\"])
   if you pass in only a driver and coll, every odd element should be an WebElement"
  ([driver coll]
   {:pre [(even? (count coll))]}
   (doseq [[elm val] (partition 2 coll)] (set-element driver elm val)))
  ([driver lookup-type coll]
   {:pre [(even? (count coll))]}
   (doseq [[elm val] (partition 2 coll)]
     (set-element driver lookup-type (if (keyword? elm) (name elm) elm) val))))

(defn attr
  "returns the value of an element's attribute"
  ([webelement attribute]
   (cond
     (= attribute :text)
     (.getText webelement)
     :else
     (.getAttribute webelement (name attribute))))
  ([driver lookup-type lookup-string attribute]
   (attr (get-element driver lookup-type lookup-string) attribute)))

(defn css
  "returns the value of a webelement's css value attribute"
  ([webelement attribute]
    (.getCssValue webelement attribute))
  ([driver lookup-type lookup-string attribute]
    (css (get-element driver lookup-type lookup-string) attribute)))

(defn get-element-value
  "gets the value of an element.
  :text for text and :value for value"
  ([webelement attribute]
  (cond
    (= attribute :text)
      (.getText webelement)
    (= attribute :value)
      (. webelement getAttribute "value")
    :else
      (get-element-value webelement :value)))

  ([driver lookup-type lookup-string attribute]
  (get-element-value (get-element driver lookup-type lookup-string) attribute)))

(defn click
  "clicks an element"
  ([webelement]
  (.click webelement))

  ([driver webelement]
    (scroll-into-view driver webelement)
    (click webelement))

  ([driver lookup-type & lookup-strings]
   (doseq [lookup lookup-strings]
     (click driver (get-visible-element driver lookup-type lookup)))))

(defn wait-click
  "waits with timeout (seconds) for element then clicks"
  ([driver lookup-type lookup-string timeout]
    (wait-for-element driver lookup-type lookup-string timeout)
    (click driver lookup-type lookup-string))
  ([driver lookup-type lookup-string]
    (wait-click driver lookup-type lookup-string 10)))

(defn try-click
  "repeatedly trys to click webelement until no exception occurs or
   the timeout (seconds) expires"
  ([driver lookup-type lookup-string timeout]
   (wait-for
    #(try (click driver lookup-type lookup-string) true
          (catch Exception e false))
    (* 1000 timeout) 500))
  ([driver lookup-type lookup-string]
   (try-click driver lookup-type lookup-string 10)))

(defn wait-q
  "returns a list of elements.
  Does not return elements until the query contains visible elements
  require-visible determines if the final seq returned are only visible
  elements matching the query"
  ([driver lookup-type lookup-string timeout require-visible]
   (wait-for (fn [] (not (empty? (filter #(is-visible %)
     (get-elements driver lookup-type
                   lookup-string)))))
     (* 1000 timeout) 500)
     (let [elements (if require-visible
                      (filter #(is-visible %)
                        (get-elements driver lookup-type
                                      lookup-string))
                      (get-elements driver lookup-type lookup-string))]
       (cond (empty? elements) nil
             (= 1 (count elements)) (first elements)
             :else elements)))
  ([driver lookup-type lookup-string timeout]
   (wait-q driver lookup-type lookup-string timeout false))
  ([driver lookup-type lookup-string]
   (wait-q driver lookup-type lookup-string 10)))

(defn switch-to-alert
  [driver]
  (wait-for
    (fn [] (try (.alert (.switchTo driver)) true (catch Exception e false))) 2000 20)
  (.alert (.switchTo driver)))

(defn alert-text
  "returns the text contained in a js alert box"
  [driver]
  (let [result (.getText (switch-to-alert driver))]
    (Thread/sleep 10) result))

(defn alert-accept
  "accepts alert"
  [driver]
  (.accept (switch-to-alert driver))
  (Thread/sleep 10))

(defn alert-dismiss
  "dismisses an alert"
  [driver]
  (.dismiss (switch-to-alert driver))
  (Thread/sleep 10))

; unfortunately this is broken in chrome at the moment so it won't be supported
;(defn alert-input
;  [driver s]
;  (.sendKeys (.alert (.switchTo driver)) s))

(defn iframe
  "switches to iframe by index or webelement (via calling (get-element lookup-type lookup-string))"
  ([driver n]
   (.frame (.switchTo driver) n))

  ([driver lookup-type lookup-string]
   (.frame (.switchTo driver) (get-element driver lookup-type lookup-string))))

(defn iframe-parent
  "switches to parent iframe"
  [driver]
  (.parentFrame (.switchTo driver)))

(defn iframe-default
  "switches to default content (main body of the html that contains all the iframes)"
  [driver]
  (.defaultContent (.switchTo driver)))

(defn cookie
  "Creates or retrieves a cookie named cookie-name.
   Sets cookie's value to cookie-value if provided"
  ([driver cookie-name cookie-value]
    (.addCookie (.manage driver) (org.openqa.selenium.Cookie. cookie-name
                                                              cookie-value)))
  ([driver cookie-name]
    (.getValue (.getCookieNamed (.manage driver) cookie-name))))

(defn screen-shot
  [driver output]
  (io/copy (.getScreenshotAs (cast org.openqa.selenium.TakesScreenshot driver)
                             (org.openqa.selenium.OutputType/FILE))
           (io/file output)))
