(ns qrity.png-integration-test
  "Integration evidence for the clj-png-adapter seam, run only via the
  :png-integration alias (or an equivalent bb/nbb classpath) because it
  needs the submodule's sources on the classpath.

  The full circle uses no platform image API on any runtime: generate a
  symbol, render grey samples, encode PNG octets, decode them back, and
  scan the luminance image to the original payload."
  (:require [png-adapter.decode :as png-decode]
            [png-adapter.encode :as png-encode]
            [qrity.encode :as encode]
            [qrity.image :as image]
            [qrity.render :as render]
            [qrity.scan :as scan]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer [deftest is testing]])))

(def ^:private payload "Příliš žluťoučký PNG — https://example.com/qr?id=7")

(defn- scan-decoded-png
  [decoded-png]
  (scan/decode-luminance-image
   (image/interleaved->luminance-image (:width decoded-png)
                                       (:height decoded-png)
                                       (:channels decoded-png)
                                       (:samples decoded-png))))

(deftest png-round-trip-needs-no-platform-image-api
  (let [symbol-value (encode/encode-text payload :m)
        request (render/render-grey-samples (:matrix symbol-value) 4)
        decoded-png (png-decode/decode-octets
                     (png-encode/encode-octets request))
        result (scan-decoded-png decoded-png)]
    (testing "the PNG is the natural 1-bit greyscale form"
      (is (= 1 (:bit-depth decoded-png)))
      (is (= :greyscale (:colour-type decoded-png))))
    (testing "the payload survives the full circle"
      (is (= payload (:payload result)))
      (is (= (:version symbol-value) (:version result))))))

(deftest reflectance-reversed-png-round-trips
  (let [symbol-value (encode/encode-text payload :m)
        request (render/render-grey-samples (:matrix symbol-value) 4
                                            render/default-quiet-zone
                                            {:inverted? true})
        result (scan-decoded-png
                (png-decode/decode-octets
                 (png-encode/encode-octets request)))]
    (is (= payload (:payload result)))
    (is (true? (:inverted? result)))))
