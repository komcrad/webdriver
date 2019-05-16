(ns webdriver.driver-manager-test
  (:require [clojure.test :refer :all]
            [webdriver.driver-manager :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [digest :as digest]
            [komcrad-utils.io :as kio]))
(deftest download-driver-test
  (testing "download-driver"
    (.delete (download-driver {:driver-type :chrome}))
    (is (.exists (download-driver {:driver-type :chrome})))
    (.delete (download-driver {:driver-type :firefox}))
    (is (.exists (download-driver {:driver-type :firefox})))))
