# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.10.0 2019-02-15]
## Added
- better file download support

## [0.9.0 2019-02-04]
## Added
- enabled? function so people don't have to use java interop or the is-visible function to see if an element is enabled

## [0.8.1 2019-01-31]
## Updated
- browser version used for circle ci
- driver versions so newer browsers will be supported

## [0.8.0 2019-01-30]
## Added
- sibling and parent functions for traversing the dom
- select-elm functions for dealing with select elements

## [0.7.0 - 2019-01-10]
### Added
- insert-html and delete-elm functions for basic dom manipulation
- html function: a wrapper around the html macro in the hiccup library
- wait-for-trans function to allow waiting on transitions to finish
- options function for getting available options of a select element

## [0.6.2 - 2018-11-21]
### Fixed/Added
- Added to-localhost function that allows firefox to local localhost without throwing an exception

## [0.6.1 - 2018-11-14]
### Fixed
- Firefox is now able to set-file-input

## [0.6.0 - 2018-10-26]
### Added
- with-webdriver: a macro to replace with-driver with better syntax
- poll-interval option for wait-for-element
- visible? function
### Changed
- Updated version of chromedriver to 2.43
- Updated geckdriver to 0.23.0
- Updated circleci to use latest webdriver docker image containing chrome 70 and firefox 62
### Fixed
- is-visible docstring

## [0.5.3 - 2018-09-21]
### Changed
- Updates version of chromedriver to 2.42 
- Update circleci to use new webdriver docker image that contains updated web browsers

## [0.5.2 - 2018-09-04]
### Fixed
- Specifies version of chromedriver that will be used

## [0.5.1 - 2018-08-21]
### Fixed
- Issue where --headless chromedriver would not ignore insecure certs
- missing function wait-elm-dom. A function to wait for elment to attach to dom without needing to be clickable.

## [0.5.0 - 2018-08-03]
### Added
- New set-elms function that gives the option to use a different syntax for setting multiple elements
## [0.4.0 - 2018-06-25]
### Added
- functions for dealing with invisible and unclickable elements (wait-q and try-click)

## [0.3.2 - 2018-05-29]
### Fixed
- annoying chromedriver startup logging

## [0.3.1 - 2018-05-04]
### Fixed
- an issue with scroll-into-view not scrolling to center of view

## [0.3.0 - 2018-05-03]
### Added
- added scroll-into-view in all functions where element being visible is important
- scroll-into-view function
- css function
- attr function
- wait-click function
- ability to click multiple elements by passing in multiple element selectors to core/click

## [0.2.5 - 2018-02-05]
### Added
- function to set file input elements
- wrapper around sendKeys
- function for taking screenshots of the driver

## [0.2.4 - 2018-01-24]
### Added
- Waits for alerts to exist for up to 2 seconds and waits 10ms for the alert to close 
- Downloads go to current directory (for firefox there is a whitelist of MIME types that will download without prompt)
- Chrome will always ignore cert errors

## [0.2.3 - 2018-01-23]
### Changed
- Changes how elements are set so they will trigger key-up events

## [0.2.2 - 2018-01-19]
### Added
- set-elements option to query for elements

## [0.2.1 - 2018-01-17]
### Added
- get-visible-element (function that uses get-elements and returns the first visible element)

### Changed
- reverted 0.2.0 changes
- click, set-element, and clear all use get-visible-element for queries


## [0.2.0 - 2018-01-17]
### Changed
- the get-element function will now only return an element if it is visible.. side-effects include only being able to call (click driver :name "button") (set driver :name "input" "value") on visible elements. You can still pass in webelements. To functions that use get-element

## [0.1.1] - 2017-12-06
### Added
- Added cookie support

## 0.1.0 - 2017-12-05
### Added
- Released a basic clojure wrapper for the selenium library

[Unreleased]: https://github.com/komcrad/webdriver/compare/0.1.1...HEAD
[0.2.0]: https://github.com/your-name/webdriver/compare/0.1.1...0.2.0
[0.1.1]: https://github.com/komcrad/webdriver/compare/0.1.0...0.1.1
