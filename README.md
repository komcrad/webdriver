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
(click driver :xpath "//input[@value ='Google Search'][1]")
```

etc...

core.clj contains functions to handle common browser tasks. You can either read through that or checkout tests/webdriver/core_test.clj for unit test examples.

## Tested versions of firefox and chrome
- Firefox: 61.0.1
- Google Chrome 68.0.3440.106

## Known issues
- chromedriver does not handle alerts properly when headless
