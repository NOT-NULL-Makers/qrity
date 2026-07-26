(ns qrity.matrix
  (:require [clojure.spec.alpha :as s]
            [qrity.metadata :as metadata]
            [qrity.parameters :as parameters]))

(def version-1-size 21)

(def construction-cells
  #{:unset :reserved :reserved-light :reserved-dark})

(defn- empty-matrix
  [dimension]
  (vec (repeat dimension
               (vec (repeat dimension :unset)))))

(defn empty-version-1-matrix
  []
  (empty-matrix version-1-size))

(defn- in-bounds?
  [dimension coordinate]
  (<= 0 coordinate (dec dimension)))

(defn- reserved-module
  [dark?]
  (if dark? :reserved-dark :reserved-light))

(defn- draw-finder-pattern
  [matrix center-row center-column]
  (let [dimension (count matrix)]
    (reduce
     (fn [matrix [row column]]
       (if (and (in-bounds? dimension row)
                (in-bounds? dimension column))
         (let [distance (max (abs (- row center-row))
                             (abs (- column center-column)))]
           (assoc-in matrix [row column]
                     (reserved-module
                      (and (not= distance 2)
                           (not= distance 4)))))
         matrix))
     matrix
     (for [row (range (- center-row 4) (+ center-row 5))
           column (range (- center-column 4) (+ center-column 5))]
       [row column]))))

(def primary-format-coordinates
  [[8 0] [8 1] [8 2] [8 3] [8 4] [8 5] [8 7] [8 8]
   [7 8] [5 8] [4 8] [3 8] [2 8] [1 8] [0 8]])

(def secondary-format-coordinates
  [[8 20] [8 19] [8 18] [8 17] [8 16] [8 15] [8 14] [8 13]
   [14 8] [15 8] [16 8] [17 8] [18 8] [19 8] [20 8]])

(defn- fail-collision!
  [pattern coordinate existing replacement]
  (throw
   (ex-info "Function-pattern construction collision"
            {:qrity/error :function-pattern-collision
             :pattern pattern
             :coordinate coordinate
             :existing existing
             :replacement replacement
             :clause "7.7.2"})))

(defn- write-unset
  [matrix pattern coordinate cell]
  (let [existing (get-in matrix coordinate)]
    (if (= :unset existing)
      (assoc-in matrix coordinate cell)
      (fail-collision! pattern coordinate existing cell))))

(defn- secondary-format-coordinates-for
  [dimension]
  (into []
        (concat
         (for [column (range (dec dimension) (- dimension 9) -1)]
           [8 column])
         (for [row (range (- dimension 7) dimension)]
           [row 8]))))

(defn- version-information-reservation-coordinates
  [dimension]
  {:top-right
   (into []
         (for [row (range 6)
               column (range (- dimension 11) (- dimension 8))]
           [row column]))
   :bottom-left
   (into []
         (for [row (range (- dimension 11) (- dimension 8))
               column (range 6)]
           [row column]))})

(defn- draw-timing-patterns
  [matrix]
  (let [dimension (count matrix)
        coordinates
        (concat
         (for [column (range 8 (- dimension 8))]
           [[6 column] (even? column)])
         (for [row (range 8 (- dimension 8))]
           [[row 6] (even? row)]))]
    (reduce
     (fn [matrix [coordinate dark?]]
       (write-unset matrix
                    :timing
                    coordinate
                    (reserved-module dark?)))
     matrix
     coordinates)))

(defn- draw-alignment-pattern
  [matrix [center-row center-column]]
  (reduce
   (fn [matrix [row column]]
     (let [coordinate [row column]
           distance (max (abs (- row center-row))
                         (abs (- column center-column)))
           cell (reserved-module (not= distance 1))
           existing (get-in matrix coordinate)]
       (cond
         (= :unset existing)
         (assoc-in matrix coordinate cell)

         (and (or (= row 6) (= column 6))
              (= existing cell))
         matrix

         :else
         (fail-collision! :alignment coordinate existing cell))))
   matrix
   (for [row (range (- center-row 2) (+ center-row 3))
         column (range (- center-column 2) (+ center-column 3))]
     [row column])))

(defn- alignment-pattern-coordinates
  [dimension center-axes]
  (let [last-center (- dimension 7)
        finder-overlaps #{[6 6] [6 last-center] [last-center 6]}]
    (into []
          (remove finder-overlaps)
          (for [row center-axes
                column center-axes]
            [row column]))))

(defn- reserve-coordinates
  [matrix pattern coordinates]
  (reduce #(write-unset %1 pattern %2 :reserved)
          matrix
          coordinates))

(defn- build-function-matrix
  [version]
  (let [{:keys [dimension alignment-pattern-centers]}
        (parameters/ordinary-qr-parameters version :l)
        finder-centers
        [[3 3] [3 (- dimension 4)] [(- dimension 4) 3]]
        matrix
        (reduce (fn [matrix [row column]]
                  (draw-finder-pattern matrix row column))
                (empty-matrix dimension)
                finder-centers)
        matrix (draw-timing-patterns matrix)
        alignment-centers
        (alignment-pattern-coordinates
         dimension
         alignment-pattern-centers)
        matrix
        (reduce draw-alignment-pattern matrix alignment-centers)
        matrix
        (reserve-coordinates
         matrix
         :format-information
         (concat primary-format-coordinates
                 (secondary-format-coordinates-for dimension)))
        matrix
        (if (<= 7 version)
          (let [{:keys [top-right bottom-left]}
                (version-information-reservation-coordinates dimension)]
            (reserve-coordinates
             matrix
             :version-information
             (concat top-right bottom-left)))
          matrix)]
    ;; ISO/IEC 18004:2015, Clause 7.9.1: (4V+9, 8) = (N−8, 8).
    (write-unset matrix
                 :fixed-dark
                 [(- dimension 8) 8]
                 :reserved-dark)))

(defn function-matrix?
  [value]
  (and
   (vector? value)
   (try
     (let [dimension (count value)
           version (when (and (<= 21 dimension 177)
                              (zero? (mod (- dimension 17) 4)))
                     (quot (- dimension 17) 4))
           profile
           (when version
             (parameters/ordinary-qr-parameters version :l))]
       (and
        profile
        (= dimension (:dimension profile))
        (every? #(and (vector? %)
                      (= dimension (count %))
                      (every? construction-cells %))
                value)
        (not-any? #{:light :dark} (mapcat identity value))
        (= (+ (* 8 (:total-codeword-count profile))
              (:remainder-bit-count profile))
           (count (filter #{:unset} (mapcat identity value))))
        (= value (build-function-matrix version))))
     (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) _
       false))))

(s/def ::function-matrix function-matrix?)

(defn function-matrix
  "Builds an ordinary-QR function-pattern and metadata-reservation template.

  The quiet zone is excluded. The zero-argument form remains fixed to Version 1.
  Format and version-information modules are reserved but unresolved."
  ([]
   (function-matrix 1))
  ([version]
   (build-function-matrix version)))

(s/fdef function-matrix
  :args (s/alt :fixed (s/cat)
               :selected (s/cat :version ::parameters/version))
  :ret ::function-matrix)

(defn- inferred-version
  [matrix]
  (when (vector? matrix)
    (let [dimension (count matrix)]
      (when (and (<= 21 dimension 177)
                 (zero? (mod (- dimension 17) 4)))
        (quot (- dimension 17) 4)))))

(defn- require-function-matrix!
  [matrix]
  (when-not (function-matrix? matrix)
    (throw
     (ex-info
      "Placement requires an exact canonical function-pattern template"
      {:qrity/error :invalid-function-matrix
       :reason :noncanonical-template
       :actual-dimension (when (vector? matrix) (count matrix))
       :clause "7.7.3"}))))

(defn- traverse-data-coordinates
  [matrix]
  (let [dimension (count matrix)]
    (loop [right-column (dec dimension)
           upward? true
           coordinates []]
      (if (< right-column 1)
        coordinates
        (let [right-column (if (= right-column 6) 5 right-column)
              rows (if upward?
                     (range (dec dimension) -1 -1)
                     (range dimension))
              pair-coordinates
              (for [row rows
                    column [right-column (dec right-column)]
                    :when (= :unset (get-in matrix [row column]))]
                [row column])]
          (recur (- right-column 2)
                 (not upward?)
                 (into coordinates pair-coordinates)))))))

(defn data-coordinates
  "Returns the Clause 7.7.3 placement traversal for a canonical template."
  [matrix]
  (require-function-matrix! matrix)
  (traverse-data-coordinates matrix))

(defn bit-vector?
  [value]
  (and (vector? value)
       (pos? (count value))
       (every? #{0 1} value)))

(defn place-data
  "Places a complete final-message bit vector into a canonical function template.

  The result is unmasked and still contains unresolved metadata reservations."
  [matrix message-bits]
  (require-function-matrix! matrix)
  (when-not (bit-vector? message-bits)
    (throw
     (ex-info
      "Message bits must be a non-empty vector containing only 0 and 1"
      {:qrity/error :invalid-message-bits
       :reason (cond
                 (not (vector? message-bits)) :not-vector
                 (empty? message-bits) :empty-bits
                 :else :invalid-bit)
       :message-bits message-bits
       :clause "7.7.3"})))
  (let [version (inferred-version matrix)
        coordinates (traverse-data-coordinates matrix)]
    (when-not (= (count coordinates) (count message-bits))
      (throw
       (ex-info
        "Message bit count does not fill the selected encoding region"
        {:qrity/error :message-bit-count-mismatch
         :version version
         :expected-count (count coordinates)
         :actual-count (count message-bits)
         :coordinate-count (count coordinates)
         :bit-count (count message-bits)
         :clause "7.7.3"})))
    (let [remainder-bit-count
          (:remainder-bit-count
           (parameters/ordinary-qr-parameters version :l))]
      (when (and (pos? remainder-bit-count)
                 (not-every? zero?
                             (take-last remainder-bit-count message-bits)))
        (throw
         (ex-info
          "Remainder bits must be zero before data masking"
          {:qrity/error :invalid-remainder-bits
           :reason :nonzero-remainder-bit
           :version version
           :remainder-bit-count remainder-bit-count
           :remainder-bits
           (vec (take-last remainder-bit-count message-bits))
           :clause "7.7.3"}))))
    (let [placed-matrix
          (reduce (fn [matrix [coordinate bit]]
                    (assoc-in matrix
                              coordinate
                              (if (zero? bit) :light :dark)))
                  matrix
                  (map vector coordinates message-bits))]
      (when (some #{:unset} (mapcat identity placed-matrix))
        (throw
         (ex-info
          "Placement left encoding modules unresolved"
          {:qrity/error :placement-invariant-failure
           :version version
           :invariant :unset-modules-remain
           :clause "7.7.3"})))
      {:matrix placed-matrix
       :data-coordinates coordinates})))

(s/def ::message-bits bit-vector?)
(s/def ::data-coordinate
  (s/and vector?
         #(= 2 (count %))
         #(every? (fn [coordinate]
                    (and (int? coordinate)
                         (<= 0 coordinate 176)))
                  %)))
(s/def ::data-coordinates
  (s/coll-of ::data-coordinate :kind vector? :distinct true :min-count 1))

(def placement-keys
  #{:matrix :data-coordinates})

(defn placed-matrix?
  [value]
  (and
   (vector? value)
   (let [version (inferred-version value)]
     (when version
       (let [template (build-function-matrix version)
             coordinates (traverse-data-coordinates template)
             coordinate-set (set coordinates)
             dimension (count template)]
         (and
          (= dimension (count value))
          (every? #(and (vector? %)
                        (= dimension (count %)))
                  value)
          (not-any? #{:unset} (mapcat identity value))
          (every?
           (fn [[row column]]
             (let [coordinate [row column]
                   cell (get-in value coordinate)]
               (if (contains? coordinate-set coordinate)
                 (#{:light :dark} cell)
                 (= (get-in template coordinate) cell))))
           (for [row (range dimension)
                 column (range dimension)]
             [row column]))))))))

(defn placement-structure?
  "Checks self-contained placement shape and canonical reserved cells.

  Use `placement-matches-message-bits?` to verify identity with source bits."
  [value]
  (and (map? value)
       (= placement-keys (set (keys value)))
       (placed-matrix? (:matrix value))
       (let [version (inferred-version (:matrix value))
             template (when version (build-function-matrix version))]
         (= (:data-coordinates value)
            (when template (traverse-data-coordinates template))))))

(defn placement-matches-message-bits?
  "Checks a structurally valid placement against its source message bits."
  [placement message-bits]
  (and
   (placement-structure? placement)
   (bit-vector? message-bits)
   (= message-bits
      (mapv #(case (get-in (:matrix placement) %)
               :light 0
               :dark 1)
            (:data-coordinates placement)))))

(defn placement-request?
  [{:keys [matrix message-bits]}]
  (and
   (function-matrix? matrix)
   (bit-vector? message-bits)
   (= (count message-bits)
      (count (traverse-data-coordinates matrix)))
   (let [version (inferred-version matrix)
         remainder-bit-count
         (:remainder-bit-count
          (parameters/ordinary-qr-parameters version :l))]
     (every? zero? (take-last remainder-bit-count message-bits)))))

(s/def ::placed-matrix placed-matrix?)
(s/def ::placement-structure placement-structure?)
(s/def ::placement-request
  (s/and
   (s/cat :matrix any? :message-bits any?)
   placement-request?))

(s/fdef data-coordinates
  :args (s/cat :matrix ::function-matrix)
  :ret ::data-coordinates
  :fn (fn [{:keys [args ret]}]
        (= ret (traverse-data-coordinates (:matrix args)))))

(s/fdef place-data
  :args ::placement-request
  :ret ::placement-structure
  :fn
  (fn [{:keys [args ret]}]
    (let [{:keys [matrix message-bits]} args
          coordinates (:data-coordinates ret)]
      (and
       (= coordinates (traverse-data-coordinates matrix))
       (placement-matches-message-bits? ret message-bits)))))

(defn format-information-bits
  "Returns ordinary-QR format information, most significant bit first.

  The one-argument form preserves the fixed level M API."
  ([mask-reference]
   (metadata/format-information-bits mask-reference))
  ([error-correction-level mask-reference]
   (metadata/format-information-bits
    error-correction-level
    mask-reference)))

(defn add-format-information
  [matrix mask-reference]
  (let [most-significant-first
        (format-information-bits mask-reference)
        least-significant-first
        (vec (reverse most-significant-first))
        format-cell (fn [bit]
                      (if (zero? bit)
                        :reserved-light
                        :reserved-dark))]
    (reduce (fn [matrix [coordinate bit]]
              (assoc-in matrix coordinate (format-cell bit)))
            matrix
            (concat (map vector
                         primary-format-coordinates
                         most-significant-first)
                    (map vector
                         secondary-format-coordinates
                         least-significant-first)))))

(defn- version-information-placement-coordinates
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

(defn- metadata-ready-matrix-for-version?
  [value version]
  (let [template (build-function-matrix version)
        dimension (count template)]
    (and
     (= dimension (count value))
     (every? #(and (vector? %)
                   (= dimension (count %)))
             value)
     (every?
      (fn [[row column]]
        (let [template-cell (get-in template [row column])
              cell (get-in value [row column])]
          (case template-cell
            :unset (#{:light :dark} cell)
            :reserved (= :reserved cell)
            (= template-cell cell))))
      (for [row (range dimension)
            column (range dimension)]
        [row column])))))

(defn metadata-ready-matrix?
  "Checks a canonical placed/masked construction matrix with unresolved metadata."
  [value]
  (and
   (vector? value)
   (when-let [version (inferred-version value)]
     (metadata-ready-matrix-for-version? value version))))

(defn- data-mask-condition?
  [mask-reference row column]
  (let [product (* row column)]
    (case mask-reference
      0 (even? (+ row column))
      1 (even? row)
      2 (zero? (mod column 3))
      3 (zero? (mod (+ row column) 3))
      4 (even? (+ (quot row 2)
                  (quot column 3)))
      5 (zero? (+ (mod product 2)
                  (mod product 3)))
      6 (even? (+ (mod product 2)
                  (mod product 3)))
      7 (even? (+ (mod (+ row column) 2)
                  (mod product 3))))))

(defn- toggle-module
  [cell]
  (if (= :light cell) :dark :light))

(defn- apply-data-mask*
  [matrix mask-reference]
  (mapv
   (fn [row-index row]
     (mapv
      (fn [column-index cell]
        (if (and (#{:light :dark} cell)
                 (data-mask-condition?
                  mask-reference
                  row-index
                  column-index))
          (toggle-module cell)
          cell))
      (range)
      row))
   (range)
   matrix))

(defn apply-data-mask
  "Applies one explicit Table 10 mask as a reversible encoding-region transform.

  The returned matrix is structurally metadata-ready, but its shape alone cannot
  prove which mask was applied or whether multiple masks were composed."
  [matrix mask-reference]
  (when-not (metadata-ready-matrix? matrix)
    (throw
     (ex-info
      "Data masking requires an exact placed matrix with unresolved metadata"
      {:qrity/error :invalid-data-mask-matrix
       :reason :noncanonical-metadata-ready-matrix
       :actual-dimension (when (vector? matrix) (count matrix))
       :clause "7.8.1"})))
  (when-not (s/valid? ::parameters/mask-reference mask-reference)
    (throw
     (ex-info
      "Ordinary QR mask reference must be an integer from 0 through 7"
      {:qrity/error :invalid-mask-reference
       :mask-reference mask-reference
       :clause "7.8.2"})))
  (apply-data-mask* matrix mask-reference))

(defn data-mask-application-matches?
  "Checks the exact relation between a placed matrix, result, and mask reference.

  This relational predicate does not infer mask provenance from the result alone."
  [before after mask-reference]
  (and
   (metadata-ready-matrix? before)
   (metadata-ready-matrix? after)
   (s/valid? ::parameters/mask-reference mask-reference)
   (= after (apply-data-mask* before mask-reference))))

(defn apply-mask-2
  "Compatibility wrapper for ordinary QR data mask reference 010."
  [matrix]
  (apply-data-mask matrix 2))

(defn- metadata-complete-matrix-for-version?
  [value version]
  (let [template (build-function-matrix version)
        dimension (count template)]
    (and
     (= dimension (count value))
     (every? #(and (vector? %)
                   (= dimension (count %)))
             value)
     (every?
      (fn [[row column]]
        (let [template-cell (get-in template [row column])
              cell (get-in value [row column])]
          (case template-cell
            :unset (#{:light :dark} cell)
            :reserved (#{:reserved-light :reserved-dark} cell)
            (= template-cell cell))))
      (for [row (range dimension)
            column (range dimension)]
        [row column])))))

(defn metadata-complete-matrix?
  "Checks a canonical construction matrix whose metadata reservations are resolved."
  [value]
  (and
   (vector? value)
   (when-let [version (inferred-version value)]
     (metadata-complete-matrix-for-version? value version))))

(defn- write-information-bits
  [matrix coordinates information-bits]
  (reduce (fn [matrix [coordinate bit]]
            (assoc-in matrix
                      coordinate
                      (if (zero? bit)
                        :reserved-light
                        :reserved-dark)))
          matrix
          (map vector coordinates information-bits)))

(defn- resolve-metadata*
  [matrix error-correction-level mask-reference]
  (let [version (inferred-version matrix)
        dimension (count matrix)
        format-bits
        (metadata/format-information-bits
         error-correction-level
         mask-reference)
        matrix
        (-> matrix
            (write-information-bits
             primary-format-coordinates
             format-bits)
            (write-information-bits
             (secondary-format-coordinates-for dimension)
             (vec (reverse format-bits))))
        matrix
        (if (< version 7)
          matrix
          (let [version-bits
                (vec
                 (reverse
                  (metadata/version-information-bits version)))
                {:keys [top-right bottom-left]}
                (version-information-placement-coordinates dimension)]
            (-> matrix
                (write-information-bits top-right version-bits)
                (write-information-bits bottom-left version-bits))))]
    matrix))

(defn resolve-metadata
  "Atomically resolves ordinary-QR format and applicable version reservations.

  The matrix supplies the version. The level and mask reference are explicit, but
  this low-level function cannot prove that the encoding modules were masked with
  that reference; later orchestration must bind those operations."
  [matrix error-correction-level mask-reference]
  (when-not (metadata-ready-matrix? matrix)
    (throw
     (ex-info
      "Metadata resolution requires an exact metadata-ready construction matrix"
      {:qrity/error :invalid-metadata-matrix
       :reason :noncanonical-metadata-ready-matrix
       :actual-dimension (when (vector? matrix) (count matrix))
       :clause "7.9.1/7.10"})))
  (resolve-metadata*
   matrix
   error-correction-level
   mask-reference))

(defn metadata-resolution-matches?
  "Checks a completed matrix against its metadata-ready source and parameters."
  [before after error-correction-level mask-reference]
  (and
   (metadata-ready-matrix? before)
   (metadata-complete-matrix? after)
   (s/valid? ::parameters/error-correction-level
             error-correction-level)
   (s/valid? ::metadata/mask-reference mask-reference)
   (= after
      (resolve-metadata*
       before
       error-correction-level
       mask-reference))))

(s/def ::metadata-ready-matrix metadata-ready-matrix?)
(s/def ::metadata-complete-matrix metadata-complete-matrix?)

(defn- data-mask-request?
  [{:keys [matrix mask-reference]}]
  (and
   (metadata-ready-matrix? matrix)
   (s/valid? ::parameters/mask-reference mask-reference)))

(s/def ::data-mask-request
  (s/and
   (s/cat :matrix any? :mask-reference any?)
   data-mask-request?))

(defn- metadata-request?
  [{:keys [matrix error-correction-level mask-reference]}]
  (and
   (metadata-ready-matrix? matrix)
   (s/valid? ::parameters/error-correction-level
             error-correction-level)
   (s/valid? ::metadata/mask-reference mask-reference)))

(s/def ::metadata-request
  (s/and
   (s/cat :matrix any?
          :error-correction-level any?
          :mask-reference any?)
   metadata-request?))

(s/fdef format-information-bits
  :args
  (s/alt :fixed-level
         (s/cat :mask-reference ::metadata/mask-reference)
         :explicit-level
         (s/cat :error-correction-level
                ::parameters/error-correction-level
                :mask-reference ::metadata/mask-reference))
  :ret ::metadata/format-information-bits)

(s/fdef apply-data-mask
  :args ::data-mask-request
  :ret ::metadata-ready-matrix
  :fn
  (fn [{:keys [args ret]}]
    (data-mask-application-matches?
     (:matrix args)
     ret
     (:mask-reference args))))

(s/fdef apply-mask-2
  :args (s/cat :matrix ::metadata-ready-matrix)
  :ret ::metadata-ready-matrix
  :fn
  (fn [{:keys [args ret]}]
    (data-mask-application-matches?
     (:matrix args)
     ret
     2)))

(s/fdef resolve-metadata
  :args ::metadata-request
  :ret ::metadata-complete-matrix
  :fn
  (fn [{:keys [args ret]}]
    (metadata-resolution-matches?
     (:matrix args)
     ret
     (:error-correction-level args)
     (:mask-reference args))))

(defn final-bit-matrix
  "Converts a fully resolved construction matrix to public 0/1 modules."
  [matrix]
  (mapv (fn [row]
          (mapv (fn [cell]
                  (case cell
                    (:dark :reserved-dark) 1
                    (:light :reserved-light) 0
                    (throw (ex-info "Unresolved module in final matrix"
                                    {:cell cell}))))
                row))
        matrix))
