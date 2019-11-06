# webdriver

A simple (features as needs arise) Clojure wrapper of Selenium Webdriver

## Usage

If you want to use the truly headless/recording environment, you'll need to be running a linux host with xvfb and ffmpeg installed.  
`sudo apt-get install ffmpeg xvfb`  
`sudo pacman -S ffmpeg xorg-server-xvfb`

## Example
### Assuming you're in a repl (`lein repl`)...

Import the core namespace:

`(use 'webdriver.core)`

Create a driver object

Chrome:
- `(def driver (create-driver {:driver-type :chrome}))`
- `(def driver (create-driver :chrome []))` (old method)

Firefox:
- `(def driver (create-driver {:driver-type :firefox}))`
- `(def driver (create-driver :firefox []))` (old method)

Headless:
- `(def driver (create-driver {:driver-type :chrome :driver-args ["--headless"]}))`
- `(def driver (create-driver :chrome ["--headless"]))` (old method)

Xvfb (Linux only with xvfb installed)

```
(require '[webdriver.screen :as scr])
(def screen (scr/start-screen)) ;this is separate from driver so that we can have multiple drivers run in a single frame buffer
(def driver (create-driver {:driver-type :chrome :xvfb-port (:xvfb-port screen)}))
```

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

If you want a truly headless environment that doesn't have the issues of geckodriver --headless or chromedriver --headless:
```
(scr/with-screen [screen]
  (with-webdriver [driver :driver-type :chrome :xvfb-port (:xvfb-port screen)]
    (to driver "https://google.com")
    (set-element driver :name "q" "silly memes")
    (click (wait-for-element driver :xpath "//input[@value = 'Google Search'][1]"))))
```

If you want to record that headless session:
```
(scr/with-recorded-screen [screen :vid-out "/tmp/webdriver.mp4"]
  (with-webdriver [driver :driver-type :chrome :xvfb-port (:xvfb-port screen)]
    (to driver "https://google.com")
    (set-element driver :name "q" "silly memes")
    (click (wait-for-element driver :xpath "//input[@value = 'Google Search'][1]"))))
```

etc...

core.clj contains functions to handle common browser tasks. You can either read through that or checkout tests/webdriver/core_test.clj for unit test examples.

## Breaking changes:
  - In webdriver 0.11.0, functions that use to return webdrivers now return maps with a WebDriver object at :driver. This means that some code that used java interop on driver objects will now be broken. Internally this has been updated. According to the unit tests, the functions in webdriver.core still work together properly.
  ```
  ; in webdriver 0.10.0 this would work
  (def driver (create-driver {:driver-type :chrome}))
  (.getVersion (.getCapabilities driver))
  (driver-quit driver)

  ; in webdriver 0.11.0 you'll have to do it differently
  (def driver (create-driver {:driver-type :chrome}))
  (.getVersion (.getCapabilities (:driver driver))) ; notice (:driver driver) instead of just driver
  (driver-quit driver)
  ```

## Running unit tests in docker image locally
```
docker run -v /dev/shm/:/dev/shm -it wsbu/webdriver:latest /bin/bash
git clone https://github.com/wsbu/webdriver.git
cd webdriver
lein test
```

## Tested versions of firefox and chrome
- webdriver 0.14.3
  - Firefox 68.0.1
  - Chromium 78.0.3904.70
- webdriver 0.14.0
  - Firefox 68.0.1
  - Chromium 76.0.3809.87
- webdriver 0.13.0
  - Firefox 67.0.1
  - Chromium 75.0.3770.80
- webdriver 0.12.0
  - Firefox 66.0.1
  - Google Chrome 74.0.3729.108
- webdriver 0.11.0
  - Firefox 66.0
  - Google Chrome 73.0.3683.86
- webdriver 0.10.0
  - Firefox 66.0
  - Firefox 65.0.1
  - Google Chrome 73.0.3683.86
  - Google Chrome 73.0.3683.75
- webdriver 0.8.1, 0.9.0, 0.10.0
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
