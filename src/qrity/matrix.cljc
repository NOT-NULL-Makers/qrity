(ns qrity.matrix
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
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

(defn- version-information-coordinates
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
                (version-information-coordinates dimension)]
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

(defn- require-version-1-matrix!
  [matrix]
  (when-not (and (vector? matrix)
                 (= version-1-size (count matrix))
                 (every? #(and (vector? %)
                               (= version-1-size (count %)))
                         matrix))
    (throw
     (ex-info
      "This placement API accepts only a Version 1 construction matrix"
      {:qrity/error :unsupported-matrix-dimension
       :expected-dimension version-1-size
       :actual-dimension (when (vector? matrix) (count matrix))
       :clause "7.7.3"}))))

(defn data-coordinates
  "Returns the Clause 7.7.3 Version 1 placement traversal in bit order."
  [matrix]
  (require-version-1-matrix! matrix)
  (loop [right-column 20
         upward? true
         coordinates []]
    (if (< right-column 1)
      coordinates
      (let [right-column (if (= right-column 6) 5 right-column)
            rows (if upward?
                   (range 20 -1 -1)
                   (range 0 21))
            pair-coordinates
            (for [row rows
                  column [right-column (dec right-column)]
                  :when (= :unset (get-in matrix [row column]))]
              [row column])]
        (recur (- right-column 2)
               (not upward?)
               (into coordinates pair-coordinates))))))

(defn place-data
  [matrix message-bits]
  (let [coordinates (data-coordinates matrix)]
    (when-not (= (count coordinates) (count message-bits))
      (throw (ex-info "Message does not fill the Version 1 encoding region"
                      {:coordinate-count (count coordinates)
                       :bit-count (count message-bits)})))
    {:matrix
     (reduce (fn [matrix [coordinate bit]]
               (assoc-in matrix coordinate (if (zero? bit) :light :dark)))
             matrix
             (map vector coordinates message-bits))
     :data-coordinates coordinates}))

(defn apply-mask-2
  "Applies data mask reference 010 only to placed encoding modules."
  [matrix]
  (mapv (fn [row]
          (mapv (fn [column-index cell]
                  (if (and (#{:light :dark} cell)
                           (zero? (mod column-index 3)))
                    (if (= :light cell) :dark :light)
                    cell))
                (range)
                row))
        matrix))

(defn- format-information-value
  [mask-reference]
  ;; Table 12 maps level M to 00; the three low data bits are the mask.
  (let [data mask-reference
        shifted (bit-shift-left data 10)
        generator 0x537
        remainder
        (reduce (fn [value bit-index]
                  (if (bit-test value bit-index)
                    (bit-xor value
                             (bit-shift-left generator (- bit-index 10)))
                    value))
                shifted
                (range 14 9 -1))]
    (bit-xor (bit-or shifted remainder) 0x5412)))

(defn format-information-bits
  [mask-reference]
  (bits/unsigned-integer->bits
   (format-information-value mask-reference)
   15))

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
