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

(defn- luminance-values
  [image]
  (plane/values (:luminance image)))

(deftest interleaved-samples-flatten-to-luminance
  (testing "one channel passes grey through"
    (is (= [10 20 30 40]
           (luminance-values
            (image/interleaved->luminance-image 2 2 1 [10 20 30 40])))))
  (testing "two channels keep grey and ignore alpha"
    (is (= [10 20]
           (luminance-values
            (image/interleaved->luminance-image 2 1 2 [10 255 20 0])))))
  (testing "three channels apply the BT.601 integer weights"
    (is (= [(quot (+ (* 299 200) (* 587 100) (* 114 50)) 1000) 0 255]
           (luminance-values
            (image/interleaved->luminance-image
             3 1 3 [200 100 50 0 0 0 255 255 255])))))
  (testing "four channels agree with three and ignore alpha"
    (is (= (luminance-values
            (image/interleaved->luminance-image 1 1 3 [200 100 50]))
           (luminance-values
            (image/interleaved->luminance-image 1 1 4 [200 100 50 7]))))))

(deftest interleaved-samples-validate-shape
  (testing "an unknown channel count"
    (is (= :invalid-channel-count
           (:qrity/error
            (exception-data
             #(image/interleaved->luminance-image 1 1 5 [1 2 3 4 5]))))))
  (testing "a sample count that does not match the dimensions"
    (is (= :invalid-sample-count
           (:qrity/error
            (exception-data
             #(image/interleaved->luminance-image 2 2 3 [1 2 3])))))))
