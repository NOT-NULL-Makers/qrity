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
    (is (zero? (:format-hamming-distance decoded)))
    (is (zero? (:corrected-error-count decoded)))
    (is (= (:matrix symbol-value) (:reconstructed-matrix decoded)))))

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

(deftest corrects-a-damaged-data-region-and-reconstructs-the-symbol
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)
        damaged (flip-module matrix [12 12])
        decoded (decode/decode-matrix damaged)]
    (is (= "8675309" (:payload decoded)))
    (is (= 1 (:corrected-error-count decoded)))
    (is (= matrix (:reconstructed-matrix decoded))
        "the repaired symbol is the pristine matrix, re-encoded")))

(deftest corrects-scattered-damage-up-to-several-codewords
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)
        damaged (reduce flip-module
                        matrix
                        [[9 2] [12 12] [15 18] [18 10]])
        decoded (decode/decode-matrix damaged)]
    (is (= "8675309" (:payload decoded)))
    (is (<= 1 (:corrected-error-count decoded) 4))
    (is (= matrix (:reconstructed-matrix decoded)))))

(defn- erase-modules
  [bit-matrix coordinates]
  (reduce (fn [matrix coordinate]
            (assoc-in matrix coordinate nil))
          bit-matrix
          coordinates))

(deftest treats-unknown-modules-as-erasures
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)
        ;; Unknown modules scattered across the placement so they touch
        ;; more codewords than the five-error limit could absorb if they
        ;; counted as errors.
        unknown (erase-modules matrix
                               [[9 0] [9 10] [9 20] [12 4] [12 15]
                                [15 2] [15 12] [18 18]])
        decoded (decode/decode-matrix unknown)]
    (is (= "8675309" (:payload decoded)))
    (is (<= 6 (:corrected-erasure-count decoded) 8)
        "the unknowns must span more codewords than the error capacity")
    (is (= matrix (:reconstructed-matrix decoded)))))

(deftest corrects-mixed-unknown-and-flipped-modules
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)
        damaged (-> matrix
                    (erase-modules (for [row (range 9 12)
                                         column (range 0 3)]
                                     [row column]))
                    (flip-module [15 16]))
        decoded (decode/decode-matrix damaged)]
    (is (= "8675309" (:payload decoded)))
    (is (<= 1 (:corrected-error-count decoded)))
    (is (<= 1 (:corrected-erasure-count decoded)))
    (is (= matrix (:reconstructed-matrix decoded)))))

(deftest refuses-damage-beyond-the-correction-capacity
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)
        damaged (reduce flip-module
                        matrix
                        ;; Scattered across the data region so more than
                        ;; five of Version 1-M's 26 codewords are hit.
                        [[9 0] [9 4] [9 10] [9 16] [9 20]
                         [12 2] [12 12] [12 18]
                         [15 4] [15 10] [15 16]
                         [18 2] [18 12] [18 19]])
        failure (exception-data #(decode/decode-matrix damaged))]
    (is (= :uncorrectable-message (:qrity/error failure)))
    (is (= [0] (:uncorrectable-block-indexes failure)))))

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
