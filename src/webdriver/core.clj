(ns webdriver.core
  (:gen-class)
  (:import
    [org.openqa.selenium.remote RemoteWebDriver]
    [org.openqa.selenium WebElement])
  (:require [webdriver.driver-manager :as dm]))

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

(defmacro with-all-drivers
  "Same as with-driver but evaluates the forms in body agains all supported driver types.
   See examples in the unit tests"
  [driver-args & body]
  `(do (with-driver :chrome ~driver-args ~@body)
       (with-driver :firefox ~driver-args ~@body)))

(defn to
  "Navigates driver to the given url"
  ([driver url] (. driver get url)))

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

(defn clear
  "clears webelement"
  ([driver webelement]
  (.sendKeys webelement
    (into-array CharSequence
                [(org.openqa.selenium.Keys/chord
                 (into-array CharSequence [(. org.openqa.selenium.Keys CONTROL) "a"]))]))
  (.sendKeys webelement
    (into-array CharSequence
    [(. org.openqa.selenium.Keys BACK_SPACE)])))

  ([driver lookup-type lookup-string]
  (clear driver (get-element driver lookup-type lookup-string))))

(defn implicit-wait
  "sets driver's implicit wait timeout (in seconds)"
  [driver timeout]
  (.implicitlyWait (.timeouts (.manage driver)) timeout (java.util.concurrent.TimeUnit/SECONDS)))

(defn wait-for-element
  "Explicitly waits for element to be clickable with a timeout of max-wait (seconds)"
  ([driver lookup-type lookup-string max-wait]
  (. (new org.openqa.selenium.support.ui.WebDriverWait driver max-wait) until
     (. org.openqa.selenium.support.ui.ExpectedConditions elementToBeClickable (by lookup-type lookup-string))))
  ([driver lookup-type lookup-string]
   (wait-for-element driver lookup-type lookup-string 10)))

(defn is-visible
  "Returns true if element is visible"
  ([element]
  (try (and (.isEnabled element) (.isDisplayed element))
       (catch Exception e false)))

  ([driver lookup-type lookup-string]
  (try (is-visible (get-element driver lookup-type lookup-string))
       (catch Exception e false))))

(defn input-text
  "sets the value of a text input. If clear-element, element will be cleared before
  setting the text input"
  [driver webelement s clear-element]
    (if (= "class java.lang.String" (.toString (type webelement)))
      (input-text driver (get-element driver webelement) s clear-element)
      (do (if clear-element
            (clear driver webelement))
          (.sendKeys webelement (into-array CharSequence [s]))
          (.sendKeys webelement (into-array CharSequence
                                            [(. org.openqa.selenium.Keys CONTROL)]))
          (unfocus driver)
          webelement)))

(defn set-element
  "sets element e to value s. For select or input elements"
  ([driver e s]
  (if (= "select" (.getTagName e))
   (do
     (. (new org.openqa.selenium.support.ui.Select e)
        selectByVisibleText s)
     e)
   (do
     (clear driver e)
     (.sendKeys e (into-array CharSequence [s]))
     e)))
  ([driver lookup-type lookup-string s]
  (set-element driver (get-element driver lookup-type lookup-string) s)))

(defn set-elements
  "sets coll of elements e to coll of values v"
  [driver e v]
  (if (= (count e) (count v))
    (do
      (loop [elements e values v]
        (if (> (count elements) 0)
         (do
           (set-element driver (first elements) (first values))
           (recur (rest elements) (rest values))))))))

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

  ([driver lookup-type lookup-string]
   (click (get-element driver lookup-type lookup-string))))

(defn alert-text
  "returns the text contained in a js alert box"
  [driver]
  (.getText (.alert (.switchTo driver))))

(defn alert-accept
  "accepts alert"
  [driver]
  (.accept (.alert (.switchTo driver))))

(defn alert-dismiss
  "dismisses an alert"
  [driver]
  (.dismiss(.alert (.switchTo driver))))

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
