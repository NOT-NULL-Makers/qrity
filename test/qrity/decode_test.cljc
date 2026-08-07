(ns qrity.decode-test
  (:require [qrity.decode :as decode]
            [qrity.encode :as encode]
            [qrity.matrix :as matrix]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn- round-trip
  [symbol-value]
  (decode/decode-matrix (:matrix symbol-value)))

(deftest decodes-a-numeric-symbol-back-to-its-payload
  (let [payload "86753090000000000000000000000000000"
        symbol-value (encode/encode-numeric payload :m)
        decoded (round-trip symbol-value)]
    (is (= payload (:payload decoded)))
    (is (= :numeric (:mode decoded)))
    (is (= (count payload) (:character-count decoded)))
    (is (= (:version symbol-value) (:version decoded)))
    (is (= :m (:error-correction-level decoded)))
    (is (= (:mask-reference symbol-value) (:mask-reference decoded)))
    (is (zero? (:format-hamming-distance decoded)))))

(deftest decodes-leading-zero-numeric-groups-faithfully
  (let [payload "0012000400089"]
    (is (= payload
           (:payload (round-trip (encode/encode-numeric payload :q)))))))

(deftest decodes-an-alphanumeric-symbol-back-to-its-payload
  (let [payload "HTTPS://EXAMPLE.COM/QR/A-1 $%*+./:"
        decoded (round-trip (encode/encode-alphanumeric payload :q))]
    (is (= payload (:payload decoded)))
    (is (= :alphanumeric (:mode decoded)))))

(deftest decodes-a-byte-symbol-back-to-its-octets
  (let [octets [0 1 127 128 255 72 101 106]
        decoded (round-trip (encode/encode-byte octets :m))]
    (is (= octets (:octets decoded)))
    (is (= :byte (:mode decoded)))))

(deftest decodes-an-iso-8859-1-symbol-back-to-its-text
  (let [payload "https://example.com/search?q=qr_code&lang=cs"
        decoded (round-trip (encode/encode-iso-8859-1 payload :m))]
    (is (= payload (:payload decoded)))
    (is (= :byte (:mode decoded)))))

(deftest decodes-a-multi-block-higher-version-symbol
  (let [payload (apply str (take 220 (cycle "0123456789")))
        symbol-value (encode/encode-numeric payload :h)
        decoded (round-trip symbol-value)]
    (is (<= 7 (:version symbol-value))
        "the fixture must exercise interleaving and a version >= 7 layout")
    (is (= payload (:payload decoded)))
    (is (= (:version symbol-value) (:version decoded)))))

(deftest decodes-every-error-correction-level
  (doseq [level [:l :m :q :h]]
    (testing (str "level " level)
      (let [decoded (round-trip (encode/encode-numeric "31415926535" level))]
        (is (= "31415926535" (:payload decoded)))
        (is (= level (:error-correction-level decoded)))))))

(defn- flip-module
  [bit-matrix coordinate]
  (update-in bit-matrix coordinate #(- 1 %)))

(deftest tolerates-correctable-format-information-damage
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)
        dimension (count matrix)
        damaged (reduce flip-module
                        matrix
                        (concat
                         (take 3 matrix/primary-format-coordinates)
                         [[8 (dec dimension)]
                          [8 (- dimension 2)]
                          [8 (- dimension 3)]]))
        decoded (decode/decode-matrix damaged)]
    (is (= "8675309" (:payload decoded)))
    (is (= 3 (:format-hamming-distance decoded)))))

(deftest refuses-a-damaged-data-region-honestly
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)
        damaged (flip-module matrix [12 12])
        failure (exception-data #(decode/decode-matrix damaged))]
    (is (= :corrupted-message (:qrity/error failure)))
    (is (= :error-correction-not-implemented (:reason failure)))))

(deftest refuses-unreadable-format-information
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)
        dimension (count matrix)
        damaged (reduce flip-module
                        matrix
                        (concat
                         (take 5 matrix/primary-format-coordinates)
                         [[8 (dec dimension)] [8 (- dimension 2)]
                          [8 (- dimension 3)] [8 (- dimension 4)]
                          [8 (- dimension 5)]]))]
    (is (= :unreadable-format-information
           (:qrity/error (exception-data #(decode/decode-matrix damaged)))))))

(deftest refuses-non-matrix-input
  (is (= :invalid-matrix
         (:qrity/error (exception-data #(decode/decode-matrix [[1 0] [0 2]])))))
  (is (= :invalid-matrix
         (:qrity/error (exception-data #(decode/decode-matrix "matrix"))))))

(deftest refuses-a-dimension-matching-no-version
  (let [not-a-symbol (vec (repeat 23 (vec (repeat 23 0))))]
    (is (= :invalid-symbol-dimension
           (:qrity/error
            (exception-data #(decode/decode-matrix not-a-symbol)))))))
