(ns qrity.matrix
  (:require [qrity.bits :as bits]))

(def version-1-size 21)

(defn empty-version-1-matrix
  []
  (vec (repeat version-1-size
               (vec (repeat version-1-size :unset)))))

(defn- in-bounds?
  [coordinate]
  (<= 0 coordinate (dec version-1-size)))

(defn- reserved-module
  [dark?]
  (if dark? :reserved-dark :reserved-light))

(defn- draw-finder-pattern
  [matrix center-row center-column]
  (reduce
   (fn [matrix [row column]]
     (if (and (in-bounds? row) (in-bounds? column))
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
     [row column])))

(def primary-format-coordinates
  [[8 0] [8 1] [8 2] [8 3] [8 4] [8 5] [8 7] [8 8]
   [7 8] [5 8] [4 8] [3 8] [2 8] [1 8] [0 8]])

(def secondary-format-coordinates
  [[8 20] [8 19] [8 18] [8 17] [8 16] [8 15] [8 14] [8 13]
   [14 8] [15 8] [16 8] [17 8] [18 8] [19 8] [20 8]])

(defn function-matrix
  "Builds Version 1 finder/separator/timing patterns and format reservations."
  []
  (let [matrix (reduce (fn [matrix [row column]]
                         (draw-finder-pattern matrix row column))
                       (empty-version-1-matrix)
                       [[3 3] [3 17] [17 3]])
        matrix (reduce (fn [matrix coordinate]
                         (assoc-in matrix coordinate :reserved))
                       matrix
                       (concat primary-format-coordinates
                               secondary-format-coordinates))
        matrix (reduce (fn [matrix coordinate]
                         (assoc-in matrix coordinate :reserved-dark))
                       matrix
                       (concat
                        (for [column (range 8 13 2)] [6 column])
                        (for [row (range 8 13 2)] [row 6])))
        matrix (reduce (fn [matrix coordinate]
                         (assoc-in matrix coordinate :reserved-light))
                       matrix
                       (concat
                        (for [column (range 9 13 2)] [6 column])
                        (for [row (range 9 13 2)] [row 6])))]
    ;; ISO/IEC 18004:2015, Clause 7.9.1: fixed dark module at (4V+9, 8).
    (assoc-in matrix [13 8] :reserved-dark)))

(defn data-coordinates
  "Returns the Clause 7.7.3 Version 1 placement traversal in bit order."
  [matrix]
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
