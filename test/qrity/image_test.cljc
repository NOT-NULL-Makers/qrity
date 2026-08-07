(ns qrity.image-test
  (:require [qrity.image :as image]
            [qrity.image-fixtures :as fixtures]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(deftest binarizes-dark-pixels-below-the-midpoint
  (let [bitmap (image/binarize {:width 2
                                :height 2
                                :luminance [0 255 100 200]})]
    (is (= {:width 2 :height 2 :bits [1 0 1 0]} bitmap))))

(deftest refuses-a-contrast-free-image
  (is (= :insufficient-contrast
         (:qrity/error
          (exception-data
           #(image/binarize {:width 2
                             :height 1
                             :luminance [128 130]}))))))

(deftest refuses-a-malformed-luminance-image
  (doseq [binarize [image/binarize image/binarize-adaptive]]
    (is (= :invalid-luminance-image
           (:qrity/error
            (exception-data
             #(binarize {:width 2 :height 2 :luminance [0 255]})))))))

(deftest adaptive-binarization-matches-global-on-an-even-raster
  (let [image (fixtures/matrix->luminance-image [[1 0 1]
                                                 [0 1 0]
                                                 [1 0 1]]
                                                16 1)]
    (is (= (image/binarize image)
           (image/binarize-adaptive image)))))

(deftest adaptive-binarization-survives-a-lighting-gradient
  (let [matrix [[1 0 1 0 1]
                [0 1 0 1 0]
                [1 0 1 0 1]
                [0 1 0 1 0]
                [1 0 1 0 1]]
        shaded (fixtures/shade-image
                (fixtures/matrix->luminance-image matrix 16 1))
        expected (:bits (image/binarize
                         (fixtures/matrix->luminance-image matrix 16 1)))
        adaptive (:bits (image/binarize-adaptive shaded))
        global (:bits (image/binarize shaded))
        disagreements (fn [bits]
                        (count (filter true? (map not= expected bits))))]
    (testing "the global threshold misreads the darkened side"
      (is (< 100 (disagreements global))))
    (testing "local black points recover the true modules"
      (is (zero? (disagreements adaptive))))))
