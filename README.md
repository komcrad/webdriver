# webdriver

A simplified (far from fully featured) Clojure wrapper of Selenium Webdriver

## Usage
[![Clojars Project](https://img.shields.io/clojars/v/webdriver.svg)](https://clojars.org/webdriver)
[![CircleCI](https://circleci.com/gh/komcrad/webdriver/tree/master.svg?style=svg&circle-token=a5fcd5b0389dd482ec5e55fb3c6bab0715377cd9)](https://circleci.com/gh/komcrad/webdriver/tree/master)

## Example
### Assuming you're in a repl (`lein repl`)...

Import the core namespace:

`(use 'webdriver.core)`

Create a driver object

Chrome:

`(def driver (create-driver :chrome []))`

Firefox:

`(def driver (create-driver :firefox []))`

Headless:

`(def driver (create-driver :chrome ["--headless"]))`

Now you can pass the driver object into the other functions in core to manipulate it.

```
(to driver "https://google.com")
(set-element driver :name "q" "silly memes")
(click (wait-for-element driver :xpath "//input[@value = 'Google Search'][1]"))
```

From functional code:
```
(with-webdriver [driver :driver-type :chrome :driver-args ["--headless"]]
  (to driver "https://google.com")
  (set-element driver :name "q" "silly memes")
  (click (wait-for-element driver :xpath "//input[@value = 'Google Search'][1]")))
```

etc...

core.clj contains functions to handle common browser tasks. You can either read through that or checkout tests/webdriver/core_test.clj for unit test examples.

## Tested versions of firefox and chrome
- webdriver 0.8.1
  - Firefox 65.0
  - Google Chrome 72.0.3626.81
- webdriver 0.7.0
  - Firefox 63.0.3
  - Google Chrome 71.0.3578.80
- webdriver 0.6.0
  - Firefox: 62.0.3
  - Google Chrome 70.0.3538.67
- webdriver 0.5.3
  - Firefox: 62.0
  - Google Chrome 69.0.3497.100
- webdriver 0.5.2
  - Firefox: 61.0.1
  - Google Chrome 68.0.3440.106
