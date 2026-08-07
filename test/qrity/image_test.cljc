(ns qrity.image-test
  (:require [qrity.encode :as encode]
            [qrity.image :as image]
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
  (is (= :invalid-luminance-image
         (:qrity/error
          (exception-data
           #(image/binarize {:width 2 :height 2 :luminance [0 255]}))))))

(deftest refuses-a-dark-region-that-is-not-a-symbol
  (let [luminance (vec (repeat 100 fixtures/light-luminance))
        blob (reduce (fn [pixels index] (assoc pixels index 0))
                     luminance
                     [44 45 54 55])]
    (is (= :no-symbol-found
           (:qrity/error
            (exception-data
             #(image/decode-luminance-image {:width 10
                                             :height 10
                                             :luminance blob})))))))

(deftest decodes-rendered-symbols-across-scales-and-quiet-zones
  (let [payload "IMAGE ROUND TRIP 42"
        {:keys [matrix version mask-reference]}
        (encode/encode-alphanumeric payload :m)]
    (doseq [[pixel-scale quiet-zone] [[1 4] [3 4] [5 2] [8 0]]]
      (testing (str "scale " pixel-scale ", quiet zone " quiet-zone)
        (let [decoded (image/decode-luminance-image
                       (fixtures/matrix->luminance-image
                        matrix pixel-scale quiet-zone))]
          (is (= payload (:payload decoded)))
          (is (= version (:version decoded)))
          (is (= mask-reference (:mask-reference decoded)))
          (is (= (count matrix)
                 (:dimension (:symbol-region decoded)))))))))

(deftest reports-where-the-symbol-was-found
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :l)
        decoded (image/decode-luminance-image
                 (fixtures/matrix->luminance-image matrix 4 3))]
    (is (= {:left 12 :top 12 :module-size 4.0 :dimension 21}
           (:symbol-region decoded)))))
