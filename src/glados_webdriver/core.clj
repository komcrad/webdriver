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

(defn get-elements
  "finds an element and returns WebElement"
  [driver lookup-type element-name]
  (if lookup-type
    (cond
      (= :id lookup-type)
        (. driver findElements (. org.openqa.selenium.By id element-name))
      (= :name lookup-type)
        (. driver findElements (. org.openqa.selenium.By name element-name))
      (= :linkText lookup-type)
        (. driver findElements (. org.openqa.selenium.By linkText element-name))
      (= :className lookup-type)
        (. driver findElements (. org.openqa.selenium.By className element-name))
      (= :xpath lookup-type)
        (. driver findElements (. org.openqa.selenium.By xpath element-name))
      (= :text lookup-type)
        (. driver findElements (. org.openqa.selenium.By xpath
                                (str "//*[contains(text(), '" element-name "')]")))
      (= :tagName lookup-type)
        (. driver findElements (. org.openqa.selenium.By tagName element-name))
      :else (throw (Exception. (str "get-element has no option \"" lookup-type "\""))))
    (try
      (. driver findElements (. org.openqa.selenium.By name element-name))
      (catch Exception e (throw (Exception. (str "could not find element " element-name)))))))

(defn get-element
  "returns the first element matching lookup-string"
  ([driver lookup-type lookup-string]
  (nth (get-elements driver lookup-type lookup-string) 0)))

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

(defn unfocus
  "unfocuses all elements"
  [driver]
  (.sendKeys (get-element driver :tagName "body") (into-array CharSequence [""])))

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
  [args])

(defn is-visible
  ([driver element]
  (try (and (.isEnabled element) (.isDisplayed element))
    (catch Exception e false)))
  
  ([driver lookup-type lookup-string]
  (is-visible driver (get-element driver lookup-type lookup-string))))

(defn wait-for-visible
  "needs implementation"
  [args])
  
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
     (clear e)
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
  ([driver webelement attribute]
  (cond
    (= attribute :text)
      (.getText webelement)
    (= attribute :value)
      (. webelement getAttribute "value")
    :else
      (get-element-value driver webelement :value)))
  
  ([driver lookup-type lookup-string attribute]
  (get-element-value driver (get-element driver lookup-type lookup-string) attribute)))

(defn execute-script
    "Version of execute-script that uses a WebDriver instance directly."
      [^RemoteWebDriver webdriver js & js-args]
        (.executeScript webdriver ^String js (into-array Object js-args)))

(defn scroll-into-view!
  "Scrolls the given element into view"
  [driver element]
  (let [scrollElementToMiddle
        (str "var viewPortHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);"
        "var elementTop = arguments[0].getBoundingClientRect().top;"
        "window.scrollBy(0, elementTop-(viewPortHeight/2));")]
    (execute-script driver scrollElementToMiddle element)))

(defn scroll-to
  "move to element so it is in view"
  ([driver webelement]
  ;(let [actions (new org.openqa.selenium.interactions.Actions driver)]
  ;  (. actions moveToElement webelement)
  ;  (. actions perform))
  (scroll-into-view! driver webelement))
  
  ([driver lookup-type lookup-string]
  (scroll-to driver (get-element driver lookup-type lookup-string))))

(defn click
  "clicks an element"
  ([driver webelement]
  (scroll-to driver webelement)
  (.click webelement))
  
  ([driver lookup-type lookup-string]
   (click driver (get-element driver lookup-type lookup-string))))
