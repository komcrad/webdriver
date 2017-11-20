(ns glados-webdriver.core
  (:gen-class)
  (:import
    [org.openqa.selenium.remote RemoteWebDriver]
    [org.openqa.selenium WebElement])
  (:require [glados-webdriver.driver-manager :as dm]))

(defn create-driver
  ([driver-type args]
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
  ([driver-type driver-args & body]
  (list 'let (vector 'driver (list 'create-driver driver-type driver-args))
        (list 'try (cons 'do body) '(catch Exception e (throw e)) '(finally (driver-quit driver))))))

(defmacro with-all-drivers
  [driver-args & body]
  `(do (with-driver :chrome ~driver-args ~@body)
       (with-driver :firefox ~driver-args ~@body)))

(defn to
  "Navigates the driver to the given url"
  ([driver url] (. driver get url)))

(defn driver-quit
  "calls quit on the driver"
  ([driver]
    (. driver quit)))

(defn by
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
  "finds an element and returns WebElement"
  [driver lookup-type element-name]
  (if lookup-type
    (. driver findElements (by lookup-type element-name))))

(defn get-element
  "returns the first element matching lookup-string"
  ([driver lookup-type lookup-string]
  (try (nth (get-elements driver lookup-type lookup-string) 0)
       (catch Exception e nil))))

(defn q
  "finds and returns webelement with name -> linkText -> id s"
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
  (.activeElement (.switchTo driver)))

(defn execute-script
    "Version of execute-script that uses a WebDriver instance directly."
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

(defn wait-for-element
  "needs implementation"
  ([driver lookup-type lookup-string max-wait]
  (. (new org.openqa.selenium.support.ui.WebDriverWait driver max-wait) until
     (. org.openqa.selenium.support.ui.ExpectedConditions elementToBeClickable (by lookup-type lookup-string))))
  ([driver lookup-type lookup-string]
   (wait-for-element driver lookup-type lookup-string 10)))

(defn is-visible
  ([element]
  (try (and (.isEnabled element) (.isDisplayed element))
       (catch Exception e false)))

  ([driver lookup-type lookup-string]
  (try (is-visible (get-element driver lookup-type lookup-string))
       (catch Exception e false))))

(defn input-text
  "sets the value of an input if clear, element will be cleared before
  sending text"
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
  "sets element e to value s"
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
  "gets the value of an element"
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
