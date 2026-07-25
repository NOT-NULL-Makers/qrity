(ns qrity.metadata-test
  (:require [clojure.spec.alpha :as s]
            [qrity.matrix :as matrix]
            [qrity.metadata :as metadata]
            [qrity.parameters :as parameters]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(def format-reference-values
  {:m [0x5412 0x5125 0x5E7C 0x5B4B
       0x45F9 0x40CE 0x4F97 0x4AA0]
   :l [0x77C4 0x72F3 0x7DAA 0x789D
       0x662F 0x6318 0x6C41 0x6976]
   :h [0x1689 0x13BE 0x1CE7 0x19D0
       0x0762 0x0255 0x0D0C 0x083B]
   :q [0x355F 0x3068 0x3F31 0x3A06
       0x24B4 0x2183 0x2EDA 0x2BED]})

(def version-reference-values
  [0x07C94 0x085BC 0x09A99 0x0A4D3 0x0BBF6 0x0C762
   0x0D847 0x0E60D 0x0F928 0x10B78 0x1145D 0x12A17
   0x13532 0x149A6 0x15683 0x168C9 0x177EC 0x18EC4
   0x191E1 0x1AFAB 0x1B08E 0x1CC1A 0x1D33F 0x1ED75
   0x1F250 0x209D5 0x216F0 0x228BA 0x2379F 0x24B0B
   0x2542E 0x26A64 0x27541 0x28C69])

(defn bits->integer
  [information-bits]
  (reduce (fn [value bit]
            (+ (* value 2) bit))
          0
          information-bits))

(defn polynomial-remainder
  [value generator]
  (let [generator-degree
        (dec
         (count
          (take-while pos?
                      (iterate #(quot % 2) generator))))]
    (loop [value value]
      (let [value-degree
            (dec
             (count
              (take-while pos?
                          (iterate #(quot % 2) value))))]
        (if (< value-degree generator-degree)
          value
          (recur
           (bit-xor
            value
            (bit-shift-left
             generator
             (- value-degree generator-degree)))))))))

(defn hamming-distance
  [left right]
  (count
   (filter true?
           (map not=
                left
                right))))

(defn cell-bit
  [cell]
  (case cell
    :reserved-light 0
    :reserved-dark 1))

(defn format-coordinate-copies
  [dimension]
  {:primary
   [[8 0] [8 1] [8 2] [8 3] [8 4] [8 5] [8 7] [8 8]
    [7 8] [5 8] [4 8] [3 8] [2 8] [1 8] [0 8]]
   :secondary
   (into []
         (concat
          (for [column (range (dec dimension) (- dimension 9) -1)]
            [8 column])
          (for [row (range (- dimension 7) dimension)]
            [row 8])))})

(defn version-coordinate-copies
  [dimension]
  {:top-right
   (mapv (fn [bit-index]
           [(quot bit-index 3)
            (+ (- dimension 11) (mod bit-index 3))])
         (range 18))
   :bottom-left
   (mapv (fn [bit-index]
           [(+ (- dimension 11) (mod bit-index 3))
            (quot bit-index 3)])
         (range 18))})

(defn metadata-coordinates
  [version dimension]
  (let [{:keys [primary secondary]}
        (format-coordinate-copies dimension)
        {:keys [top-right bottom-left]}
        (version-coordinate-copies dimension)]
    (set
     (concat primary
             secondary
             (when (<= 7 version)
               top-right)
             (when (<= 7 version)
               bottom-left)))))

(defn metadata-ready-matrix
  [version]
  (mapv
   (fn [row-index row]
     (mapv
      (fn [column-index cell]
        (if (= :unset cell)
          (if (even? (+ row-index column-index))
            :light
            :dark)
          cell))
      (range)
      row))
   (range)
   (matrix/function-matrix version)))

(defn changed-coordinates
  [before after]
  (let [dimension (count before)]
    (set
     (for [row (range dimension)
           column (range dimension)
           :when (not= (get-in before [row column])
                       (get-in after [row column]))]
       [row column]))))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(deftest all-format-information-words-match-normative-annex-c
  (let [words
        (vec
         (for [level parameters/error-correction-levels
               mask-reference (range 8)]
           (metadata/format-information-bits level mask-reference)))]
    (doseq [level parameters/error-correction-levels
            mask-reference (range 8)
            :let [information-bits
                  (metadata/format-information-bits
                   level
                   mask-reference)
                  expected
                  (get-in format-reference-values
                          [level mask-reference])
                  unmasked
                  (bit-xor (bits->integer information-bits)
                           0x5412)]]
      (testing (pr-str [level mask-reference])
        (is (= expected (bits->integer information-bits)))
        (is (= 15 (count information-bits)))
        (is (zero? (polynomial-remainder unmasked 0x537)))))
    (doseq [left-index (range (count words))
            right-index (range (inc left-index) (count words))]
      (is (<= 7
              (hamming-distance
               (nth words left-index)
               (nth words right-index)))))
    (is (= (metadata/format-information-bits :m 2)
           (metadata/format-information-bits 2)))
    (is (= [1 0 1 1 1 1 0 0 1 1 1 1 1 0 0]
           (metadata/format-information-bits :m 2)))
    (is (= [1 0 0 0 0 0 0 1 1 0 0 1 1 1 0]
           (metadata/format-information-bits :m 5)))))

(deftest all-version-information-words-match-normative-annex-d
  (let [words
        (mapv metadata/version-information-bits (range 7 41))]
    (doseq [version (range 7 41)
            :let [information-bits
                  (metadata/version-information-bits version)
                  expected
                  (nth version-reference-values (- version 7))]]
      (testing (str "Version " version)
        (is (= expected (bits->integer information-bits)))
        (is (= version
               (bits->integer (subvec information-bits 0 6))))
        (is (= 18 (count information-bits)))
        (is (zero?
             (polynomial-remainder
              (bits->integer information-bits)
              0x1F25)))))
    (doseq [left-index (range (count words))
            right-index (range (inc left-index) (count words))]
      (is (<= 8
              (hamming-distance
               (nth words left-index)
               (nth words right-index)))))
    (is (= [0 0 0 1 1 1 1 1 0 0 1 0 0 1 0 1 0 0]
           (metadata/version-information-bits 7)))))

(deftest version-seven-placement-is-pinned-to-figure-25-and-figure-28
  (let [before (metadata-ready-matrix 7)
        after (matrix/resolve-metadata before :m 2)
        format-bits-lsb-first
        [0 0 1 1 1 1 1 0 0 1 1 1 1 0 1]
        secondary-format
        [[8 44] [8 43] [8 42] [8 41] [8 40] [8 39] [8 38] [8 37]
         [38 8] [39 8] [40 8] [41 8] [42 8] [43 8] [44 8]]
        version-bits-lsb-first
        [0 0 1 0 1 0 0 1 0 0 1 1 1 1 1 0 0 0]
        top-right
        [[0 34] [0 35] [0 36]
         [1 34] [1 35] [1 36]
         [2 34] [2 35] [2 36]
         [3 34] [3 35] [3 36]
         [4 34] [4 35] [4 36]
         [5 34] [5 35] [5 36]]
        bottom-left
        [[34 0] [35 0] [36 0]
         [34 1] [35 1] [36 1]
         [34 2] [35 2] [36 2]
         [34 3] [35 3] [36 3]
         [34 4] [35 4] [36 4]
         [34 5] [35 5] [36 5]]]
    (is (= format-bits-lsb-first
           (mapv #(cell-bit (get-in after %))
                 secondary-format)))
    (is (= version-bits-lsb-first
           (mapv #(cell-bit (get-in after %))
                 top-right)))
    (is (= version-bits-lsb-first
           (mapv #(cell-bit (get-in after %))
                 bottom-left)))))

(deftest metadata-resolution-is-exhaustive-and-confined
  (doseq [version (range 1 41)
          :let [before (metadata-ready-matrix version)
                dimension (count before)
                expected-coordinates
                (metadata-coordinates version dimension)
                {:keys [primary secondary]}
                (format-coordinate-copies dimension)
                {:keys [top-right bottom-left]}
                (version-coordinate-copies dimension)]
          level parameters/error-correction-levels
          mask-reference (range 8)
          :let [after
                (matrix/resolve-metadata
                 before
                 level
                 mask-reference)
                format-bits
                (metadata/format-information-bits
                 level
                 mask-reference)]]
    (testing (pr-str [version level mask-reference])
      (is (s/valid? ::matrix/metadata-ready-matrix before))
      (is (s/valid? ::matrix/metadata-complete-matrix after))
      (is (= expected-coordinates
             (changed-coordinates before after)))
      (is (= format-bits
             (mapv #(cell-bit (get-in after %)) primary)))
      (is (= (vec (reverse format-bits))
             (mapv #(cell-bit (get-in after %)) secondary)))
      (is (= :reserved-dark
             (get-in after [(- dimension 8) 8])))
      (if (< version 7)
        (is (= 30 (count expected-coordinates)))
        (let [version-bits
              (vec
               (reverse
                (metadata/version-information-bits version)))]
          (is (= 66 (count expected-coordinates)))
          (is (= version-bits
                 (mapv #(cell-bit (get-in after %)) top-right)))
          (is (= version-bits
                 (mapv #(cell-bit (get-in after %)) bottom-left))))))))

(deftest invalid-metadata-requests-fail-structurally
  (doseq [[thunk error]
          [[#(metadata/format-information-bits :x 0)
            :invalid-error-correction-level]
           [#(metadata/format-information-bits :m -1)
            :invalid-mask-reference]
           [#(metadata/format-information-bits :m 8)
            :invalid-mask-reference]
           [#(metadata/format-information-bits :m 1.5)
            :invalid-mask-reference]
           [#(metadata/version-information-bits 0)
            :invalid-version]
           [#(metadata/version-information-bits 41)
            :invalid-version]
           [#(metadata/version-information-bits 1.5)
            :invalid-version]
           [#(metadata/version-information-bits 1)
            :version-information-not-required]
           [#(metadata/version-information-bits 6)
            :version-information-not-required]]]
    (is (= error (:qrity/error (exception-data thunk)))))
  (let [ready (metadata-ready-matrix 7)
        complete (matrix/resolve-metadata ready :m 2)
        wrong-format-bit
        (update-in complete
                   [8 0]
                   {:reserved-light :reserved-dark
                    :reserved-dark :reserved-light})
        partial (assoc-in ready [8 0] :reserved-dark)
        malformed (assoc-in ready [0 0] :reserved-light)]
    (is (matrix/metadata-resolution-matches?
         ready complete :m 2))
    (is (not
         (matrix/metadata-resolution-matches?
          ready wrong-format-bit :m 2)))
    (is (not
         (matrix/metadata-resolution-matches?
          ready complete :m 3)))
    (doseq [invalid [complete partial malformed (matrix/function-matrix 7)]]
      (is (= :invalid-metadata-matrix
             (:qrity/error
              (exception-data
               #(matrix/resolve-metadata invalid :m 2))))))
    (is (= :invalid-error-correction-level
           (:qrity/error
            (exception-data
             #(matrix/resolve-metadata ready :x 2)))))
    (is (= :invalid-mask-reference
           (:qrity/error
            (exception-data
             #(matrix/resolve-metadata ready :m 8)))))))
