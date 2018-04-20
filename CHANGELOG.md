# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
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
