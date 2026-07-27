(ns qrity.parameters
  "Ordinary QR Code parameter catalogue and isolated Numeric version selection.

  These parameters describe standard profiles. The provisional generalized Numeric
  encoder consumes them; catalogue presence is not a claim of support for other
  modes."
  (:require [clojure.spec.alpha :as s]))

(def error-correction-levels
  "Ordinary QR Code error-correction levels in Table 7 row order."
  [:l :m :q :h])

(def ^:private error-correction-level-set
  (set error-correction-levels))

;; Canonical transcriptions from ISO/IEC 18004:2015, Table 1, printed
;; pp. 19–20 (PDF pp. 27–28): [total codewords, remainder bits].
(def ^:private table-1-version-facts
  [[26 0] [44 7] [70 7] [100 7] [134 7] [172 7] [196 0] [242 0]
   [292 0] [346 0] [404 0] [466 0] [532 0] [581 3] [655 3] [733 3]
   [815 3] [901 3] [991 3] [1085 3] [1156 4] [1258 4] [1364 4]
   [1474 4] [1588 4] [1706 4] [1828 4] [1921 3] [2051 3] [2185 3]
   [2323 3] [2465 3] [2611 3] [2761 3] [2876 0] [3034 0] [3196 0]
   [3362 0] [3532 0] [3706 0]])

;; Canonical transcriptions from normative Annex E, Table E.1, printed
;; pp. 83–84 (PDF pp. 91–92). Values are row/column center axes; finder
;; overlaps are excluded only when the Cartesian placements are constructed.
(def ^:private alignment-pattern-centers
  [[]
   [6 18] [6 22] [6 26] [6 30] [6 34]
   [6 22 38] [6 24 42] [6 26 46] [6 28 50] [6 30 54] [6 32 58]
   [6 34 62]
   [6 26 46 66] [6 26 48 70] [6 26 50 74] [6 30 54 78]
   [6 30 56 82] [6 30 58 86] [6 34 62 90]
   [6 28 50 72 94] [6 26 50 74 98] [6 30 54 78 102]
   [6 28 54 80 106] [6 32 58 84 110] [6 30 58 86 114]
   [6 34 62 90 118]
   [6 26 50 74 98 122] [6 30 54 78 102 126]
   [6 26 52 78 104 130] [6 30 56 82 108 134]
   [6 34 60 86 112 138] [6 30 58 86 114 142]
   [6 34 62 90 118 146]
   [6 30 54 78 102 126 150] [6 24 50 76 102 128 154]
   [6 28 54 80 106 132 158] [6 32 58 84 110 136 162]
   [6 26 54 82 110 138 166] [6 30 58 86 114 142 170]])

;; Canonical transcriptions from Table 7, printed pp. 33–36 (PDF pp. 41–44).
;; Each version row contains L/M/Q/H pairs of
;; [data-codeword-count, printed Numeric capacity].
(def ^:private table-7-level-facts
  [[[19 41] [16 34] [13 27] [9 17]]
   [[34 77] [28 63] [22 48] [16 34]]
   [[55 127] [44 101] [34 77] [26 58]]
   [[80 187] [64 149] [48 111] [36 82]]
   [[108 255] [86 202] [62 144] [46 106]]
   [[136 322] [108 255] [76 178] [60 139]]
   [[156 370] [124 293] [88 207] [66 154]]
   [[194 461] [154 365] [110 259] [86 202]]
   [[232 552] [182 432] [132 312] [100 235]]
   [[274 652] [216 513] [154 364] [122 288]]
   [[324 772] [254 604] [180 427] [140 331]]
   [[370 883] [290 691] [206 489] [158 374]]
   [[428 1022] [334 796] [244 580] [180 427]]
   [[461 1101] [365 871] [261 621] [197 468]]
   [[523 1250] [415 991] [295 703] [223 530]]
   [[589 1408] [453 1082] [325 775] [253 602]]
   [[647 1548] [507 1212] [367 876] [283 674]]
   [[721 1725] [563 1346] [397 948] [313 746]]
   [[795 1903] [627 1500] [445 1063] [341 813]]
   [[861 2061] [669 1600] [485 1159] [385 919]]
   [[932 2232] [714 1708] [512 1224] [406 969]]
   [[1006 2409] [782 1872] [568 1358] [442 1056]]
   [[1094 2620] [860 2059] [614 1468] [464 1108]]
   [[1174 2812] [914 2188] [664 1588] [514 1228]]
   [[1276 3057] [1000 2395] [718 1718] [538 1286]]
   [[1370 3283] [1062 2544] [754 1804] [596 1425]]
   [[1468 3517] [1128 2701] [808 1933] [628 1501]]
   [[1531 3669] [1193 2857] [871 2085] [661 1581]]
   [[1631 3909] [1267 3035] [911 2181] [701 1677]]
   [[1735 4158] [1373 3289] [985 2358] [745 1782]]
   [[1843 4417] [1455 3486] [1033 2473] [793 1897]]
   [[1955 4686] [1541 3693] [1115 2670] [845 2022]]
   [[2071 4965] [1631 3909] [1171 2805] [901 2157]]
   [[2191 5253] [1725 4134] [1231 2949] [961 2301]]
   [[2306 5529] [1812 4343] [1286 3081] [986 2361]]
   [[2434 5836] [1914 4588] [1354 3244] [1054 2524]]
   [[2566 6153] [1992 4775] [1426 3417] [1096 2625]]
   [[2702 6479] [2102 5039] [1502 3599] [1142 2735]]
   [[2812 6743] [2216 5313] [1582 3791] [1222 2927]]
   [[2956 7089] [2334 5596] [1666 3993] [1276 3057]]])

;; Canonical transcriptions from Table 9, printed pp. 38–44 (PDF pp. 46–52).
;; Each version row contains L/M/Q/H pairs of
;; [total error-correction codewords, total error-correction blocks].
(def ^:private table-9-level-facts
  [[[7 1] [10 1] [13 1] [17 1]]
   [[10 1] [16 1] [22 1] [28 1]]
   [[15 1] [26 1] [36 2] [44 2]]
   [[20 1] [36 2] [52 2] [64 4]]
   [[26 1] [48 2] [72 4] [88 4]]
   [[36 2] [64 4] [96 4] [112 4]]
   [[40 2] [72 4] [108 6] [130 5]]
   [[48 2] [88 4] [132 6] [156 6]]
   [[60 2] [110 5] [160 8] [192 8]]
   [[72 4] [130 5] [192 8] [224 8]]
   [[80 4] [150 5] [224 8] [264 11]]
   [[96 4] [176 8] [260 10] [308 11]]
   [[104 4] [198 9] [288 12] [352 16]]
   [[120 4] [216 9] [320 16] [384 16]]
   [[132 6] [240 10] [360 12] [432 18]]
   [[144 6] [280 10] [408 17] [480 16]]
   [[168 6] [308 11] [448 16] [532 19]]
   [[180 6] [338 13] [504 18] [588 21]]
   [[196 7] [364 14] [546 21] [650 25]]
   [[224 8] [416 16] [600 20] [700 25]]
   [[224 8] [442 17] [644 23] [750 25]]
   [[252 9] [476 17] [690 23] [816 34]]
   [[270 9] [504 18] [750 25] [900 30]]
   [[300 10] [560 20] [810 27] [960 32]]
   [[312 12] [588 21] [870 29] [1050 35]]
   [[336 12] [644 23] [952 34] [1110 37]]
   [[360 12] [700 25] [1020 34] [1200 40]]
   [[390 13] [728 26] [1050 35] [1260 42]]
   [[420 14] [784 28] [1140 38] [1350 45]]
   [[450 15] [812 29] [1200 40] [1440 48]]
   [[480 16] [868 31] [1290 43] [1530 51]]
   [[510 17] [924 33] [1350 45] [1620 54]]
   [[540 18] [980 35] [1440 48] [1710 57]]
   [[570 19] [1036 37] [1530 51] [1800 60]]
   [[570 19] [1064 38] [1590 53] [1890 63]]
   [[600 20] [1120 40] [1680 56] [1980 66]]
   [[630 21] [1204 43] [1770 59] [2100 70]]
   [[660 22] [1260 45] [1860 62] [2220 74]]
   [[720 24] [1316 47] [1950 65] [2310 77]]
   [[750 25] [1372 49] [2040 68] [2430 81]]])

(def ordinary-qr-versions
  "Ordered catalogue of ordinary QR version and Tables 7/9 level facts.

  Dimension, version-information presence, per-block error-correction count,
  and shortest-first block groups are derived by `ordinary-qr-parameters`."
  (mapv
   (fn [index
        [total-codeword-count remainder-bit-count]
        centers
        table-7-facts
        table-9-facts]
     {:version (inc index)
      :total-codeword-count total-codeword-count
      :remainder-bit-count remainder-bit-count
      :alignment-pattern-centers centers
      :levels
      (into {}
            (map (fn [level
                      [data-codeword-count printed-numeric-capacity]
                      [error-correction-codeword-count
                       error-correction-block-count]]
                   [level
                    {:data-codeword-count data-codeword-count
                     :numeric-capacity printed-numeric-capacity
                     :error-correction-codeword-count
                     error-correction-codeword-count
                     :error-correction-block-count
                     error-correction-block-count}])
                 error-correction-levels
                 table-7-facts
                 table-9-facts))})
   (range 40)
   table-1-version-facts
   alignment-pattern-centers
   table-7-level-facts
   table-9-level-facts))

(s/def ::version (s/int-in 1 41))
(s/def ::error-correction-level error-correction-level-set)
(s/def ::mask-reference (s/int-in 0 8))
(s/def ::numeric-capacity pos-int?)
(s/def ::numeric-payload
  (s/and string? #(boolean (re-matches #"[0-9]+" %))))
(s/def ::data-codeword-count pos-int?)
(s/def ::error-correction-codeword-count pos-int?)
(s/def ::error-correction-block-count pos-int?)
(s/def ::error-correction-codeword-count-per-block pos-int?)
(s/def ::block-count pos-int?)
(s/def ::data-codeword-count-per-block pos-int?)
(s/def ::total-codeword-count pos-int?)
(s/def ::remainder-bit-count #{0 3 4 7})
(s/def ::alignment-pattern-centers
  (s/coll-of nat-int? :kind vector? :distinct true))
(defn block-group?
  [value]
  (and (map? value)
       (= #{:block-count :data-codeword-count-per-block}
          (set (keys value)))
       (s/valid? ::block-count (:block-count value))
       (s/valid? ::data-codeword-count-per-block
                 (:data-codeword-count-per-block value))))

(defn block-groups?
  [value]
  (and (vector? value)
       (<= 1 (count value) 2)
       (every? block-group? value)
       (or (= 1 (count value))
           (let [[short-group long-group] value]
             (= (inc (:data-codeword-count-per-block short-group))
                (:data-codeword-count-per-block long-group))))))

(s/def ::block-group block-group?)
(s/def ::block-groups block-groups?)

(def catalogue-row-keys
  #{:version
    :total-codeword-count
    :remainder-bit-count
    :alignment-pattern-centers
    :levels})

(def level-row-keys
  #{:data-codeword-count
    :numeric-capacity
    :error-correction-codeword-count
    :error-correction-block-count})

(defn catalogue-row?
  [value]
  (and (map? value)
       (= catalogue-row-keys (set (keys value)))
       (s/valid? ::version (:version value))
       (s/valid? ::total-codeword-count
                 (:total-codeword-count value))
       (s/valid? ::remainder-bit-count
                 (:remainder-bit-count value))
       (s/valid? ::alignment-pattern-centers
                 (:alignment-pattern-centers value))
       (= error-correction-level-set
          (set (keys (:levels value))))
       (every?
        (fn [level]
          (let [row (get-in value [:levels level])]
            (and (map? row)
                 (= level-row-keys (set (keys row)))
                 (s/valid? ::data-codeword-count
                           (:data-codeword-count row))
                 (s/valid? ::numeric-capacity
                           (:numeric-capacity row))
                 (s/valid? ::error-correction-codeword-count
                           (:error-correction-codeword-count row))
                 (s/valid? ::error-correction-block-count
                           (:error-correction-block-count row))
                 (< (:data-codeword-count row)
                    (:total-codeword-count value))
                 (= (:total-codeword-count value)
                    (+ (:data-codeword-count row)
                       (:error-correction-codeword-count row)))
                 (zero?
                  (mod (:error-correction-codeword-count row)
                       (:error-correction-block-count row))))))
        error-correction-levels)))

(s/def ::catalogue-row catalogue-row?)

(defn catalogue?
  [value]
  (and (vector? value)
       (= 40 (count value))
       (= (vec (range 1 41)) (mapv :version value))
       (every? catalogue-row? value)))

(s/def ::catalogue catalogue?)

(def parameter-keys
  #{:version
    :total-codeword-count
    :remainder-bit-count
    :alignment-pattern-centers
    :data-codeword-count
    :numeric-capacity
    :dimension
    :version-information-required?
    :error-correction-level
    :error-correction-codeword-count
    :error-correction-block-count
    :error-correction-codeword-count-per-block
    :block-groups})

(defn ordinary-qr-parameter-map?
  [value]
  (and (map? value)
       (= parameter-keys (set (keys value)))
       (s/valid? ::version (:version value))
       (s/valid? ::error-correction-level
                 (:error-correction-level value))
       (s/valid? ::total-codeword-count
                 (:total-codeword-count value))
       (s/valid? ::data-codeword-count
                 (:data-codeword-count value))
       (s/valid? ::error-correction-codeword-count
                 (:error-correction-codeword-count value))
       (s/valid? ::error-correction-block-count
                 (:error-correction-block-count value))
       (s/valid? ::error-correction-codeword-count-per-block
                 (:error-correction-codeword-count-per-block value))
       (s/valid? ::block-groups (:block-groups value))
       (s/valid? ::numeric-capacity (:numeric-capacity value))
       (s/valid? ::remainder-bit-count
                 (:remainder-bit-count value))
       (s/valid? ::alignment-pattern-centers
                 (:alignment-pattern-centers value))
       (= (+ 17 (* 4 (:version value)))
          (:dimension value))
       (= (<= 7 (:version value))
          (:version-information-required? value))
       (= (:total-codeword-count value)
          (+ (:data-codeword-count value)
             (:error-correction-codeword-count value)))
       (= (:error-correction-codeword-count value)
          (* (:error-correction-block-count value)
             (:error-correction-codeword-count-per-block value)))
       (= (:error-correction-block-count value)
          (reduce + (map :block-count (:block-groups value))))
       (= (:data-codeword-count value)
          (reduce +
                  (map #(* (:block-count %)
                           (:data-codeword-count-per-block %))
                       (:block-groups value))))
       (or (= 1 (count (:block-groups value)))
           (let [[short-group long-group] (:block-groups value)]
             (= (inc (:data-codeword-count-per-block short-group))
                (:data-codeword-count-per-block long-group))))))

(s/def ::ordinary-qr-parameters ordinary-qr-parameter-map?)

(defn- fail!
  [error message data]
  (throw (ex-info message (assoc data :qrity/error error))))

(defn- version-row
  [version]
  (when (and (int? version) (<= 1 version 40))
    (nth ordinary-qr-versions (dec version))))

(defn- derive-block-groups
  [data-codeword-count error-correction-block-count]
  (let [short-data-count
        (quot data-codeword-count error-correction-block-count)
        long-block-count
        (mod data-codeword-count error-correction-block-count)
        short-block-count
        (- error-correction-block-count long-block-count)]
    (cond-> []
      (pos? short-block-count)
      (conj {:block-count short-block-count
             :data-codeword-count-per-block short-data-count})

      (pos? long-block-count)
      (conj {:block-count long-block-count
             :data-codeword-count-per-block (inc short-data-count)}))))

(defn ordinary-qr-parameters
  "Returns catalogued ordinary-QR parameters for a version and EC level.

  Catalogue presence is not a claim that QRity can encode the selected profile."
  [version error-correction-level]
  (let [row (version-row version)]
    (when-not row
      (fail! :invalid-version
             "Ordinary QR version must be an integer from 1 through 40"
             {:version version}))
    (when-not (contains? error-correction-level-set
                         error-correction-level)
      (fail! :invalid-error-correction-level
             "Unknown ordinary QR error-correction level"
             {:error-correction-level error-correction-level
              :supported-error-correction-levels error-correction-levels}))
    (let [level-parameters (get-in row [:levels error-correction-level])
          total-codeword-count (:total-codeword-count row)
          data-codeword-count (:data-codeword-count level-parameters)
          error-correction-codeword-count
          (:error-correction-codeword-count level-parameters)
          error-correction-block-count
          (:error-correction-block-count level-parameters)]
      (when-not (= total-codeword-count
                   (+ data-codeword-count
                      error-correction-codeword-count))
        (fail! :invalid-parameter-catalogue
               "Table 1, Table 7, and Table 9 codeword totals disagree"
               {:version version
                :error-correction-level error-correction-level}))
      (when-not (zero?
                 (mod error-correction-codeword-count
                      error-correction-block-count))
        (fail! :invalid-parameter-catalogue
               "Table 9 EC codewords do not divide evenly across blocks"
               {:version version
                :error-correction-level error-correction-level}))
      (merge
       (dissoc row :levels)
       level-parameters
       {:dimension (+ 17 (* 4 version))
        :version-information-required? (<= 7 version)
        :error-correction-level error-correction-level
        :error-correction-codeword-count-per-block
        (quot error-correction-codeword-count
              error-correction-block-count)
        :block-groups
        (derive-block-groups data-codeword-count
                             error-correction-block-count)}))))

(defn numeric-capacity
  "Returns the Table 7 Numeric capacity for a catalogued version and EC level."
  [version error-correction-level]
  (:numeric-capacity
   (ordinary-qr-parameters version error-correction-level)))

(defn- smallest-version-for-count
  [character-count error-correction-level]
  (or
   (some (fn [{:keys [version levels]}]
           (when (<= character-count
                     (get-in levels
                             [error-correction-level :numeric-capacity]))
             version))
         ordinary-qr-versions)
   (fail! :payload-too-large
          "Numeric payload exceeds ordinary QR Version 40 capacity"
          {:mode :numeric
           :character-count character-count
           :error-correction-level error-correction-level
           :maximum-version 40
           :maximum-capacity
           (numeric-capacity 40 error-correction-level)})))

(defn smallest-numeric-version
  "Returns the smallest catalogued version fitting a non-empty ASCII-digit string.

  This selector remains isolated from encoding. The provisional generalized Numeric
  encoder invokes it, while the inspectable stage walkthrough remains Version 1-M."
  [digits error-correction-level]
  (when-not (and (string? digits)
                 (boolean (re-matches #"[0-9]+" digits)))
    (fail! :invalid-numeric-payload
           "Numeric payload must be a non-empty ASCII-digit string"
           {:mode :numeric
            :payload digits
            :reason
            (cond
              (not (string? digits)) :non-string-payload
              (empty? digits) :empty-payload
              :else :non-ascii-digit)}))
  (when-not (contains? error-correction-level-set
                       error-correction-level)
    (fail! :invalid-error-correction-level
           "Unknown ordinary QR error-correction level"
           {:error-correction-level error-correction-level
            :supported-error-correction-levels error-correction-levels}))
  (smallest-version-for-count (count digits) error-correction-level))

(s/fdef ordinary-qr-parameters
  :args (s/cat :version ::version
               :error-correction-level ::error-correction-level)
  :ret ::ordinary-qr-parameters)

(s/fdef numeric-capacity
  :args (s/cat :version ::version
               :error-correction-level ::error-correction-level)
  :ret ::numeric-capacity)

(s/fdef smallest-numeric-version
  :args (s/cat :digits ::numeric-payload
               :error-correction-level ::error-correction-level)
  :ret ::version)
