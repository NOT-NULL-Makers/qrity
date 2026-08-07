(ns qrity.scan-test
  (:require [qrity.decode]
            [qrity.detect]
            [qrity.encode :as encode]
            [qrity.image]
            [qrity.image-fixtures :as fixtures]
            [qrity.render]
            [qrity.scan :as scan]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(def alphanumeric-payload "IMAGE ROUND TRIP FIXTURE 42")

(defn- alphanumeric-symbol
  []
  (encode/encode-alphanumeric alphanumeric-payload :m))

(deftest decodes-clean-rasters-across-scales-and-quiet-zones
  (let [{:keys [matrix version mask-reference]} (alphanumeric-symbol)]
    (is (= 2 version)
        "the fixture must exercise the alignment-pattern path")
    (doseq [[pixel-scale quiet-zone] [[1 4] [3 4] [5 2] [8 0]]]
      (testing (str "scale " pixel-scale ", quiet zone " quiet-zone)
        (let [decoded (scan/decode-luminance-image
                       (fixtures/matrix->luminance-image
                        matrix pixel-scale quiet-zone))]
          (is (= alphanumeric-payload (:payload decoded)))
          (is (= version (:version decoded)))
          (is (= mask-reference (:mask-reference decoded)))
          (is (= (count matrix)
                 (:dimension (:detection decoded)))))))))

(deftest decodes-a-multi-block-higher-version-raster
  (let [payload (apply str (take 220 (cycle "0123456789")))
        {:keys [matrix version]} (encode/encode-numeric payload :h)
        decoded (scan/decode-luminance-image
                 (fixtures/matrix->luminance-image matrix 4 4))]
    (is (<= 7 version))
    (is (= payload (:payload decoded)))
    (is (= version (:version decoded)))))

(deftest decodes-rotated-pictures
  (let [{:keys [matrix]} (alphanumeric-symbol)
        picture (fixtures/matrix->luminance-image matrix 8 4)]
    (doseq [degrees [90 30 137 262]]
      (testing (str degrees " degrees")
        (let [decoded (scan/decode-luminance-image
                       (fixtures/rotate-image picture degrees))]
          (is (= alphanumeric-payload (:payload decoded))))))))

(deftest decodes-a-perspective-distorted-picture
  (let [{:keys [matrix]} (alphanumeric-symbol)
        picture (fixtures/matrix->luminance-image matrix 8 4)
        size (:width picture)
        warped (fixtures/perspective-image
                picture
                [[18 12] [(- size 6) 4] [(+ size 4) (- size 10)] [8 size]])
        decoded (scan/decode-luminance-image warped)]
    (is (= alphanumeric-payload (:payload decoded)))))

(deftest decodes-through-an-uneven-lighting-gradient
  (let [{:keys [matrix]} (alphanumeric-symbol)
        shaded (fixtures/shade-image
                (fixtures/matrix->luminance-image matrix 8 4))
        decoded (scan/decode-luminance-image shaded)]
    (is (= alphanumeric-payload (:payload decoded)))))

(deftest repairs-a-blotted-symbol-through-error-correction
  (let [payload "8675309"
        {:keys [matrix]} (encode/encode-numeric payload :h)
        picture (fixtures/matrix->luminance-image matrix 8 4)
        ;; A disc erased over the data region around module (12, 12):
        ;; quiet zone 4 modules, so pixel center = (4 + 12.5) * 8.
        blotted (fixtures/blot-image picture 132.0 132.0 10.0)
        decoded (scan/decode-luminance-image blotted)]
    (is (= payload (:payload decoded)))
    (is (pos? (:corrected-error-count decoded))
        "the blot must actually damage modules for this to evidence repair")
    (is (= matrix (:reconstructed-matrix decoded))
        "the repaired symbol is re-encoded, not patched")))

(deftest decodes-a-rotated-shaded-picture-with-damage
  (let [{:keys [matrix]} (encode/encode-iso-8859-1
                          "https://example.com/messy?p=1" :q)
        messy (-> (fixtures/matrix->luminance-image matrix 8 4)
                  (fixtures/blot-image 132.0 148.0 6.0)
                  (fixtures/rotate-image 15)
                  (fixtures/shade-image))
        decoded (scan/decode-luminance-image messy)]
    (is (= "https://example.com/messy?p=1" (:payload decoded)))))

(def curved-payload (apply str (take 450 (cycle "0123456789"))))

(defn- curved-picture
  []
  (let [{:keys [matrix]} (encode/encode-numeric curved-payload :m)]
    {:matrix matrix
     :picture (fixtures/bend-image
               (fixtures/matrix->luminance-image matrix 6 4)
               10)}))

(deftest decodes-a-curved-picture-through-the-alignment-grid
  (let [{:keys [picture]} (curved-picture)
        decoded (scan/decode-luminance-image picture)]
    (is (= curved-payload (:payload decoded)))
    (is (= 10 (:version decoded)))
    (is (= 6 (get-in decoded
                     [:detection :alignment-grid :located-node-count]))
        "every alignment pattern must anchor the sampling grid")))

(deftest the-alignment-grid-earns-its-place-under-curvature
  ;; The same curved picture through the global homography alone must
  ;; fail: the grid is not redundant refinement but the difference
  ;; between reading and not reading a bent symbol.
  (let [{:keys [picture]} (curved-picture)
        bitmap (qrity.image/binarize-adaptive picture)
        located (qrity.detect/locate-symbol bitmap)
        global-only (dissoc located :alignment-grid)]
    (is (= :uncorrectable-message
           (:qrity/error
            (exception-data
             #(qrity.decode/decode-matrix
               (qrity.detect/sample-grid bitmap global-only))))))
    (is (= curved-payload
           (:payload (qrity.decode/decode-matrix
                      (qrity.detect/sample-grid bitmap located)))))))

(deftest decodes-mirrored-and-inverted-pictures
  (let [{:keys [matrix]} (alphanumeric-symbol)
        picture (fixtures/matrix->luminance-image matrix 6 4)]
    (testing "straight pictures report both conditions false"
      (let [decoded (scan/decode-luminance-image picture)]
        (is (false? (:mirrored? decoded)))
        (is (false? (:inverted? decoded)))))
    (testing "a mirror image decodes transposed"
      (let [decoded (scan/decode-luminance-image
                     (fixtures/mirror-image picture))]
        (is (= alphanumeric-payload (:payload decoded)))
        (is (true? (:mirrored? decoded)))
        (is (false? (:inverted? decoded)))))
    (testing "a light-on-dark symbol decodes with reversed polarity"
      (let [decoded (scan/decode-luminance-image
                     (fixtures/invert-image picture))]
        (is (= alphanumeric-payload (:payload decoded)))
        (is (true? (:inverted? decoded)))
        (is (false? (:mirrored? decoded)))))
    (testing "mirrored, inverted, and rotated at once"
      (let [decoded (scan/decode-luminance-image
                     (-> picture
                         (fixtures/mirror-image)
                         (fixtures/invert-image)
                         (fixtures/rotate-image 30)))]
        (is (= alphanumeric-payload (:payload decoded)))
        (is (true? (:mirrored? decoded)))
        (is (true? (:inverted? decoded)))))))

(deftest inverted-rendering-round-trips
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)
        picture (fixtures/pbm->luminance-image
                 (qrity.render/render-pbm matrix 4 4 {:inverted? true}))
        decoded (scan/decode-luminance-image picture)]
    (is (= "8675309" (:payload decoded)))
    (is (true? (:inverted? decoded))
        "the renderer's light-on-dark raster reads back through the
        polarity retry")))

(deftest fails-structurally-when-no-symbol-is-present
  (is (= :no-finder-patterns-found
         (:qrity/error
          (exception-data
           #(scan/decode-luminance-image
             {:width 50
              :height 50
              :luminance (vec
                          (map (fn [index] (if (odd? index) 0 255))
                               (range 2500)))}))))))
