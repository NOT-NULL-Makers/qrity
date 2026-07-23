(ns qrity.reed-solomon)

;; ISO/IEC 18004:2015, Clause 7.5.2: x^8 + x^4 + x^3 + x^2 + 1.
(def primitive-polynomial 0x11D)

(defn gf-multiply
  "Multiplies two GF(256) elements using the QR Code primitive polynomial."
  [left right]
  (loop [multiplicand left
         multiplier right
         product 0]
    (if (zero? multiplier)
      product
      (let [product (if (odd? multiplier)
                      (bit-xor product multiplicand)
                      product)
            shifted (bit-shift-left multiplicand 1)
            multiplicand (if (>= shifted 0x100)
                           (bit-xor shifted primitive-polynomial)
                           shifted)]
        (recur multiplicand (quot multiplier 2) product)))))

(defn polynomial-multiply
  "Multiplies high-degree-first coefficient vectors over GF(256)."
  [left right]
  (reduce (fn [result [left-index left-value]]
            (reduce (fn [result [right-index right-value]]
                      (update result
                              (+ left-index right-index)
                              bit-xor
                              (gf-multiply left-value right-value)))
                    result
                    (map-indexed vector right)))
          (vec (repeat (dec (+ (count left) (count right))) 0))
          (map-indexed vector left)))

(defn generator-polynomial
  "Returns the QR generator polynomial ∏(x + α^i), i=0..degree-1."
  [degree]
  (loop [index 0
         alpha-power 1
         polynomial [1]]
    (if (= index degree)
      polynomial
      (recur (inc index)
             (gf-multiply alpha-power 2)
             (polynomial-multiply polynomial [1 alpha-power])))))

(defn error-correction-codewords
  "Returns the systematic Reed–Solomon remainder for `data-codewords`."
  [data-codewords degree]
  (let [generator (generator-polynomial degree)]
    (reduce
     (fn [remainder codeword]
       (let [factor (bit-xor codeword (first remainder))
             shifted (conj (subvec remainder 1) 0)]
         (mapv (fn [value coefficient]
                 (bit-xor value (gf-multiply factor coefficient)))
               shifted
               (subvec generator 1))))
     (vec (repeat degree 0))
     data-codewords)))
