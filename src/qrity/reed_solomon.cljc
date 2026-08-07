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

;; ---------------------------------------------------------------------------
;; Decoding — syndrome computation and error correction
;;
;; The decoding half works over the same field with the same generator roots
;; α^0 .. α^(degree-1). Correction is the textbook chain: syndromes,
;; Berlekamp–Massey for the error-locator polynomial, a Chien-style root
;; search over the received positions, and Forney's formula (with b = 0, so
;; the magnitude carries one factor of the position value) for the error
;; magnitudes. Polynomials in this half are kept lowest-degree-first, the
;; conventional orientation for these algorithms; the codeword vectors at the
;; boundary stay highest-degree-first like the encoder's.

(def ^:private exponentials
  "α^i for i in 0..254; α = 2 generates the multiplicative group."
  (vec (take 255 (iterate #(gf-multiply % 2) 1))))

(def ^:private logarithms
  (into {} (map-indexed (fn [power element] [element power]) exponentials)))

(defn- gf-inverse
  [element]
  (nth exponentials (mod (- 255 (logarithms element)) 255)))

(defn- gf-divide
  [numerator denominator]
  (gf-multiply numerator (gf-inverse denominator)))

(defn- evaluate-low-first
  [polynomial x]
  (reduce (fn [value coefficient]
            (bit-xor (gf-multiply value x) coefficient))
          0
          (rseq polynomial)))

(defn syndromes
  "Evaluates a highest-degree-first received block at the generator roots.

  Returns [S_0 ... S_(degree-1)] with S_i = R(α^i); an all-zero result means
  the block is a valid codeword."
  [received-codewords degree]
  (mapv (fn [root-power]
          (reduce (fn [value codeword]
                    (bit-xor (gf-multiply value
                                          (nth exponentials root-power))
                             codeword))
                  0
                  received-codewords))
        (range degree)))

(defn- add-scaled-shifted
  "polynomial + scale·x^shift·other, all lowest-degree-first."
  [polynomial scale shift other]
  (let [length (max (count polynomial) (+ shift (count other)))]
    (mapv (fn [index]
            (bit-xor (nth polynomial index 0)
                     (if (>= index shift)
                       (gf-multiply scale (nth other (- index shift) 0))
                       0)))
          (range length))))

(defn- error-locator-polynomial
  "Berlekamp–Massey: the minimal connection polynomial for the syndromes.

  Returns the lowest-degree-first locator σ with σ(0) = 1 and its register
  length L; L is the claimed error count and exceeds the correctable bound
  when the block is too damaged."
  [syndrome-values]
  (loop [iteration 0
         locator [1]
         register-length 0
         previous [1]
         previous-discrepancy 1
         gap 1]
    (if (= iteration (count syndrome-values))
      {:locator locator :register-length register-length}
      (let [discrepancy
            (reduce (fn [value term-index]
                      (bit-xor value
                               (gf-multiply
                                (nth locator term-index 0)
                                (nth syndrome-values
                                     (- iteration term-index)))))
                    0
                    (range (inc register-length)))]
        (cond
          (zero? discrepancy)
          (recur (inc iteration) locator register-length
                 previous previous-discrepancy (inc gap))

          (<= (* 2 register-length) iteration)
          (recur (inc iteration)
                 (add-scaled-shifted
                  locator
                  (gf-divide discrepancy previous-discrepancy)
                  gap
                  previous)
                 (- (inc iteration) register-length)
                 locator
                 discrepancy
                 1)

          :else
          (recur (inc iteration)
                 (add-scaled-shifted
                  locator
                  (gf-divide discrepancy previous-discrepancy)
                  gap
                  previous)
                 register-length
                 previous
                 previous-discrepancy
                 (inc gap)))))))

(defn- error-positions
  "Chien-style search: indexes whose position value inverts a locator root.

  The codeword at index i carries polynomial power n-1-i, so index i is an
  error position exactly when σ(α^-(n-1-i)) = 0."
  [locator block-length]
  (into []
        (filter
         (fn [index]
           (let [position-power (- block-length 1 index)
                 inverse-position (nth exponentials
                                       (mod (- 255 position-power) 255))]
             (zero? (evaluate-low-first locator inverse-position)))))
        (range block-length)))

(defn- error-magnitude
  "Forney's formula with b = 0: X·Ω(X⁻¹)/σ'(X⁻¹) at one position value."
  [evaluator locator-odd-terms position-power]
  (let [position-value (nth exponentials position-power)
        inverse-position (nth exponentials (mod (- 255 position-power) 255))
        derivative (evaluate-low-first locator-odd-terms inverse-position)]
    (gf-multiply position-value
                 (gf-divide (evaluate-low-first evaluator inverse-position)
                            derivative))))

(defn- locator-derivative
  "Formal derivative in characteristic 2: odd-degree terms shifted down."
  [locator]
  (mapv (fn [index] (nth locator index 0))
        (range 1 (max 2 (count locator)) 2)))

(defn- expand-derivative
  "Re-expands the odd-term vector so σ'(x) = Σ σ_(2k+1) x^(2k)."
  [odd-terms]
  (into []
        (mapcat (fn [term] [term 0]))
        odd-terms))

(defn- error-evaluator
  "Ω(x) = S(x)·σ(x) mod x^degree, lowest-degree-first."
  [syndrome-values locator degree]
  (mapv (fn [index]
          (reduce (fn [value term-index]
                    (bit-xor value
                             (gf-multiply
                              (nth locator term-index 0)
                              (nth syndrome-values (- index term-index) 0))))
                  0
                  (range (inc index))))
        (range degree)))

(defn correct-codewords
  "Corrects up to ⌊degree/2⌋ codeword errors in one received block.

  `received-codewords` is a complete highest-degree-first block — data
  codewords followed by `degree` parity codewords, as the QR message carries
  them. Returns

      {:codewords corrected-block
       :error-count 2
       :error-positions [4 17]}

  with `:error-count` zero and the block unchanged when the syndromes are
  already clear. Throws `:qrity/error :uncorrectable-codewords` when more
  than ⌊degree/2⌋ positions are damaged, when the locator's roots do not
  account for its degree, or when the corrected block still fails the
  syndrome check."
  [received-codewords degree]
  (let [syndrome-values (syndromes received-codewords degree)]
    (if (every? zero? syndrome-values)
      {:codewords received-codewords
       :error-count 0
       :error-positions []}
      (let [{:keys [locator register-length]}
            (error-locator-polynomial syndrome-values)
            positions (error-positions locator (count received-codewords))
            uncorrectable!
            (fn [reason]
              (throw
               (ex-info
                "The received block carries more errors than the parity can correct"
                {:qrity/error :uncorrectable-codewords
                 :reason reason
                 :error-capacity (quot degree 2)
                 :claimed-error-count register-length
                 :located-error-count (count positions)
                 :clause "7.5.2"})))]
        (when (> register-length (quot degree 2))
          (uncorrectable! :error-capacity-exceeded))
        (when-not (= register-length (count positions))
          (uncorrectable! :locator-roots-unaccounted))
        (let [evaluator (error-evaluator syndrome-values locator degree)
              odd-terms (expand-derivative (locator-derivative locator))
              corrected
              (reduce
               (fn [codewords index]
                 (update codewords index
                         bit-xor
                         (error-magnitude
                          evaluator
                          odd-terms
                          (- (count received-codewords) 1 index))))
               received-codewords
               positions)]
          (when-not (every? zero? (syndromes corrected degree))
            (uncorrectable! :correction-failed-verification))
          {:codewords corrected
           :error-count (count positions)
           :error-positions positions})))))
