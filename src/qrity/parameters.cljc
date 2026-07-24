(ns qrity.parameters
  "Ordinary QR Code parameter catalogue and isolated Numeric version selection.

  These parameters describe standard profiles. Except for Version 1-M, catalogue
  presence does not mean that QRity can yet encode the profile."
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

(def ordinary-qr-versions
  "Ordered catalogue of ordinary QR version and Table 7 level facts.

  Dimension, version-information presence, and aggregate error-correction
  codewords are intentionally derived by `ordinary-qr-parameters`."
  (mapv
   (fn [index [total-codeword-count remainder-bit-count] centers level-facts]
     {:version (inc index)
      :total-codeword-count total-codeword-count
      :remainder-bit-count remainder-bit-count
      :alignment-pattern-centers centers
      :levels
      (into {}
            (map (fn [level [data-codeword-count printed-numeric-capacity]]
                   [level
                    {:data-codeword-count data-codeword-count
                     :numeric-capacity printed-numeric-capacity}])
                 error-correction-levels
                 level-facts))})
   (range 40)
   table-1-version-facts
   alignment-pattern-centers
   table-7-level-facts))

(s/def ::version (s/int-in 1 41))
(s/def ::error-correction-level error-correction-level-set)
(s/def ::numeric-capacity pos-int?)
(s/def ::numeric-payload
  (s/and string? #(boolean (re-matches #"[0-9]+" %))))
(s/def ::data-codeword-count pos-int?)
(s/def ::error-correction-codeword-count pos-int?)
(s/def ::total-codeword-count pos-int?)
(s/def ::remainder-bit-count #{0 3 4 7})
(s/def ::alignment-pattern-centers
  (s/coll-of nat-int? :kind vector? :distinct true))

(def catalogue-row-keys
  #{:version
    :total-codeword-count
    :remainder-bit-count
    :alignment-pattern-centers
    :levels})

(def level-row-keys
  #{:data-codeword-count :numeric-capacity})

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
                 (< (:data-codeword-count row)
                    (:total-codeword-count value)))))
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
    :error-correction-codeword-count})

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
             (:error-correction-codeword-count value)))))

(s/def ::ordinary-qr-parameters ordinary-qr-parameter-map?)

(defn- fail!
  [error message data]
  (throw (ex-info message (assoc data :qrity/error error))))

(defn- version-row
  [version]
  (when (and (int? version) (<= 1 version 40))
    (nth ordinary-qr-versions (dec version))))

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
          data-codeword-count (:data-codeword-count level-parameters)]
      (merge
       (dissoc row :levels)
       level-parameters
       {:dimension (+ 17 (* 4 version))
        :version-information-required? (<= 7 version)
        :error-correction-level error-correction-level
        ;; Provisional derived total; Table 9 reconciliation is the next Phase 2 gate.
        :error-correction-codeword-count
        (- total-codeword-count data-codeword-count)}))))

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

  This selector does not invoke the encoder; Version 1-M remains the only profile
  currently produced by the complete QRity pipeline."
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
