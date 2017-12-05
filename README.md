# glados-webdriver

A simplified (far from fully featured) Clojure wrapper of Selenium Webdriver

## Usage
[![Clojars Project](https://img.shields.io/clojars/v/glados-webdriver.svg)](https://clojars.org/glados-webdriver)

## Example
### Assuming you're in a repl (`lein repl`)...

Import the core namespace:

`(use 'glados-webdriver.core)`

Create a driver object

For chrome:

`(def driver (create-driver :chrome []))`

For Firefox:

`(def driver (create-driver :firefox []))`

For headless:

`(def driver (create-driver :chrome ["--headless"]))`

Now you can pass the driver object into the other functions in core to manipulate it.

```
(to driver "https://google.com")
(set-element driver :name "q" "silly memes")
(click driver :xpath "//input[@value ='Google Search'][1]")
```

etc...

core.clj contains functions to handle common browser tasks. You can either read through that or checkout tests/glados-webdriver/core_test.clj for unit test examples.

## Known issues
- chromedriver does not handle alerts properly when headless
- chromedriver does not ignore insecure ssl certs when headless
