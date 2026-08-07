(ns qrity.image-test
  (:require [qrity.image :as image]
            [qrity.image-fixtures :as fixtures]
            [qrity.plane :as plane]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn- bitmap-values
  [{:keys [width height bits]}]
  {:width width :height height :bits (plane/values bits)})

(deftest binarizes-dark-pixels-below-the-midpoint
  (is (= {:width 2 :height 2 :bits [1 0 1 0]}
         (bitmap-values
          (image/binarize
           (image/luminance-image 2 2 [0 255 100 200]))))))

(deftest refuses-a-contrast-free-image
  (is (= :insufficient-contrast
         (:qrity/error
          (exception-data
           #(image/binarize
             (image/luminance-image 2 1 [128 130])))))))

(deftest refuses-a-malformed-luminance-image
  (testing "the constructor validates octet count and range"
    (is (= :invalid-luminance-image
           (:qrity/error
            (exception-data #(image/luminance-image 2 2 [0 255])))))
    (is (= :invalid-plane-value
           (:qrity/error
            (exception-data #(image/luminance-image 1 2 [0 256]))))))
  (testing "binarization refuses hand-built non-plane values"
    (doseq [binarize [image/binarize image/binarize-adaptive]]
      (is (= :invalid-luminance-image
             (:qrity/error
              (exception-data
               #(binarize {:width 2 :height 2 :luminance [0 255 0 255]}))))))))

(deftest adaptive-binarization-matches-global-on-an-even-raster
  (let [picture (fixtures/matrix->luminance-image [[1 0 1]
                                                   [0 1 0]
                                                   [1 0 1]]
                                                  16 1)]
    (is (= (bitmap-values (image/binarize picture))
           (bitmap-values (image/binarize-adaptive picture))))))

(deftest adaptive-binarization-survives-a-lighting-gradient
  (let [matrix [[1 0 1 0 1]
                [0 1 0 1 0]
                [1 0 1 0 1]
                [0 1 0 1 0]
                [1 0 1 0 1]]
        shaded (fixtures/shade-image
                (fixtures/matrix->luminance-image matrix 16 1))
        expected (plane/values
                  (:bits (image/binarize
                          (fixtures/matrix->luminance-image matrix 16 1))))
        adaptive (plane/values (:bits (image/binarize-adaptive shaded)))
        global (plane/values (:bits (image/binarize shaded)))
        disagreements (fn [bits]
                        (count (filter true? (map not= expected bits))))]
    (testing "the global threshold misreads the darkened side"
      (is (< 100 (disagreements global))))
    (testing "local black points recover the true modules"
      (is (zero? (disagreements adaptive))))))
