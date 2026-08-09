(ns qrity.reed-solomon
  (:require [qrity.plane :as plane]))

;; ISO/IEC 18004:2015, Clause 7.5.2: x^8 + x^4 + x^3 + x^2 + 1.
(def primitive-polynomial 0x11D)

(defn- bootstrap-multiply
  "Bit-by-bit GF(256) multiplication, used only to build the tables below.

  Table lookups replaced this as the working multiplier once profiling
  showed parity generation dominating large-symbol encoding (~69 ms of a
  Version 25 encode); the loop remains as the table builder so the tables
  stay derived from the primitive polynomial rather than transcribed."
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

(def ^:private exponentials
  "α^i for i in 0..509 as an octet plane; α = 2 generates the group.

  Doubled in length so a sum of two logarithms (at most 508) indexes
  directly, with no modular reduction in the multiply. Both tables are
  `qrity.plane` planes: every entry is an octet or a logarithm below
  255, and the packed representation reads flat on both runtimes —
  persistent-vector nth measurably lost to the old bit loop on V8."
  (let [single-cycle (vec (take 255 (iterate #(bootstrap-multiply % 2) 1)))]
    (plane/from-values (into single-cycle single-cycle))))

(def ^:private logarithms
  "log_α indexed by element; index 0 is unused — zero has no logarithm
  and every caller guards it first."
  (plane/from-values
   (reduce (fn [table power]
             (assoc table (plane/value-at exponentials power) power))
           (vec (repeat 256 0))
           (range 255))))

(defn gf-multiply
  "Multiplies two GF(256) elements via the log/antilog tables."
  [left right]
  (if (or (zero? left) (zero? right))
    0
    (plane/value-at exponentials
                    (+ (plane/value-at logarithms left)
                       (plane/value-at logarithms right)))))

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
;; α^0 .. α^(degree-1), and corrects both errors (position unknown) and
;; erasures (position known, value untrusted). The chain: syndromes; the
;; erasure locator Γ from the known positions; modified syndromes S·Γ; the
;; Sugiyama variant of the extended Euclidean algorithm for the error
;; locator σ and evaluator Ω — chosen over Berlekamp–Massey because one
;; stopping rule handles errors and erasures uniformly; a Chien-style root
;; search of the combined locator Ψ = σ·Γ over the received positions; and
;; Forney's formula (b = 0, so the magnitude carries one factor of the
;; position value) for the magnitudes. Capacity: 2·errors + erasures may
;; not exceed the parity degree. Polynomials in this half are kept
;; lowest-degree-first, the conventional orientation for these algorithms;
;; the codeword vectors at the boundary stay highest-degree-first like the
;; encoder's.

(defn- gf-inverse
  [element]
  (when (zero? element)
    (throw (ex-info "Zero has no multiplicative inverse in GF(256)"
                    {:qrity/error :zero-division})))
  (plane/value-at exponentials (- 255 (plane/value-at logarithms element))))

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
                                          (plane/value-at exponentials root-power))
                             codeword))
                  0
                  received-codewords))
        (range degree)))

(defn- trimmed
  "Drops trailing zero coefficients, keeping at least one."
  [polynomial]
  (loop [length (count polynomial)]
    (if (and (> length 1) (zero? (nth polynomial (dec length))))
      (recur (dec length))
      (subvec polynomial 0 length))))

(defn- zero-polynomial?
  [polynomial]
  (every? zero? polynomial))

(defn- polynomial-degree
  [polynomial]
  (dec (count (trimmed polynomial))))

(defn- polynomial-add
  [left right]
  (mapv (fn [index]
          (bit-xor (nth left index 0) (nth right index 0)))
        (range (max (count left) (count right)))))

(defn- polynomial-divmod
  "Divides lowest-degree-first polynomials into [quotient remainder]."
  [numerator divisor]
  (let [divisor (trimmed divisor)
        divisor-degree (dec (count divisor))
        divisor-lead (peek divisor)]
    (loop [remainder (trimmed numerator)
           quotient [0]]
      (let [remainder-degree (dec (count remainder))]
        (if (or (zero-polynomial? remainder)
                (< remainder-degree divisor-degree))
          [quotient remainder]
          (let [shift (- remainder-degree divisor-degree)
                factor (gf-divide (peek remainder) divisor-lead)
                scaled (into (vec (repeat shift 0))
                             (mapv #(gf-multiply factor %) divisor))
                quotient (if (< (count quotient) (inc shift))
                           (into quotient
                                 (repeat (- (inc shift) (count quotient)) 0))
                           quotient)]
            (recur (trimmed (polynomial-add remainder scaled))
                   (assoc quotient shift factor))))))))

(defn- erasure-locator
  "Γ(x) = ∏ (1 + X_j·x) over the erased positions' position values."
  [erasure-positions block-length]
  (reduce (fn [polynomial position]
            (polynomial-multiply
             polynomial
             [1 (plane/value-at exponentials
                     (mod (- block-length 1 position) 255))]))
          [1]
          erasure-positions))

(defn- solve-key-equation
  "Sugiyama: extended Euclid over x^degree and the modified syndromes.

  Divides down the remainder sequence until 2·deg(remainder) falls below
  degree + erasure-count; the running cofactor of the syndromes is then the
  error locator σ and the remainder is the evaluator Ω."
  [modified-syndromes degree erasure-count]
  (loop [previous-remainder (conj (vec (repeat degree 0)) 1)
         remainder (trimmed modified-syndromes)
         previous-sigma [0]
         sigma [1]]
    (if (or (zero-polynomial? remainder)
            (< (* 2 (polynomial-degree remainder))
               (+ degree erasure-count)))
      {:sigma sigma :omega remainder}
      (let [[quotient next-remainder]
            (polynomial-divmod previous-remainder remainder)]
        (recur remainder
               next-remainder
               sigma
               (polynomial-add previous-sigma
                               (polynomial-multiply quotient sigma)))))))

(defn- error-positions
  "Chien-style search: indexes whose position value inverts a locator root.

  The codeword at index i carries polynomial power n-1-i, so index i is an
  error position exactly when σ(α^-(n-1-i)) = 0."
  [locator block-length]
  (into []
        (filter
         (fn [index]
           (let [position-power (- block-length 1 index)
                 inverse-position (plane/value-at exponentials
                                       (mod (- 255 position-power) 255))]
             (zero? (evaluate-low-first locator inverse-position)))))
        (range block-length)))

(defn- error-magnitude
  "Forney's formula with b = 0: X·Ω(X⁻¹)/σ'(X⁻¹) at one position value."
  [evaluator locator-odd-terms position-power]
  (let [position-value (plane/value-at exponentials position-power)
        inverse-position (plane/value-at exponentials (mod (- 255 position-power) 255))
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

(defn- polynomial-product-modulo
  "(syndromes·polynomial) mod x^degree, lowest-degree-first."
  [syndrome-values polynomial degree]
  (mapv (fn [index]
          (reduce (fn [value term-index]
                    (bit-xor value
                             (gf-multiply
                              (nth polynomial term-index 0)
                              (nth syndrome-values (- index term-index) 0))))
                  0
                  (range (inc index))))
        (range degree)))

(defn- validate-erasure-positions!
  [erasure-positions block-length]
  (when-not (and (vector? erasure-positions)
                 (every? #(and (int? %) (< -1 % block-length))
                         erasure-positions)
                 (apply distinct? true erasure-positions))
    (throw
     (ex-info
      "Erasure positions must be distinct block indexes"
      {:qrity/error :invalid-erasure-positions
       :erasure-positions erasure-positions
       :block-length block-length
       :clause "7.5.2"}))))

(defn correct-codewords
  "Corrects codeword errors and erasures in one received block.

  `received-codewords` is a complete highest-degree-first block — data
  codewords followed by `degree` parity codewords, as the QR message carries
  them. `erasure-positions` names block indexes whose values are untrusted
  (any guess may stand in for them); twice the unknown-position errors plus
  the erasures may not exceed `degree`. Returns

      {:codewords corrected-block
       :error-count 2
       :erasure-count 3
       :error-positions [4 9 17 20 25]}

  where `:error-positions` lists every corrected index, erased or not, and
  the counts stay zero/unchanged when the syndromes are already clear.
  Throws `:qrity/error :uncorrectable-codewords` when the damage exceeds
  that capacity, when the locator's roots do not account for its degree, or
  when the corrected block still fails the syndrome check."
  ([received-codewords degree]
   (correct-codewords received-codewords degree []))
  ([received-codewords degree erasure-positions]
   (let [block-length (count received-codewords)
         erasure-count (count erasure-positions)]
     (validate-erasure-positions! erasure-positions block-length)
     (let [syndrome-values (syndromes received-codewords degree)
           uncorrectable!
           (fn [reason data]
             (throw
              (ex-info
               "The received block is damaged beyond the parity's correction capacity"
               (merge
                {:qrity/error :uncorrectable-codewords
                 :reason reason
                 :parity-degree degree
                 :erasure-count erasure-count
                 :clause "7.5.2"}
                data))))]
       (when (> erasure-count degree)
         (uncorrectable! :erasure-capacity-exceeded {}))
       (if (every? zero? syndrome-values)
         {:codewords received-codewords
          :error-count 0
          :erasure-count erasure-count
          :error-positions []}
         (let [gamma (erasure-locator erasure-positions block-length)
               modified-syndromes (polynomial-product-modulo
                                   syndrome-values gamma degree)
               {:keys [sigma omega]} (solve-key-equation
                                      modified-syndromes degree erasure-count)
               error-count (polynomial-degree sigma)]
           (when (zero? (first sigma))
             (uncorrectable! :singular-error-locator {}))
           (when (> (+ (* 2 error-count) erasure-count) degree)
             (uncorrectable! :error-capacity-exceeded
                             {:claimed-error-count error-count}))
           (let [combined-locator (trimmed
                                   (polynomial-multiply sigma gamma))
                 positions (error-positions combined-locator block-length)]
             (when-not (= (count positions)
                          (polynomial-degree combined-locator))
               (uncorrectable! :locator-roots-unaccounted
                               {:claimed-error-count error-count
                                :located-position-count (count positions)}))
             (let [odd-terms (expand-derivative
                              (locator-derivative combined-locator))
                   corrected
                   (reduce
                    (fn [codewords index]
                      (update codewords index
                              bit-xor
                              (error-magnitude
                               omega
                               odd-terms
                               (- block-length 1 index))))
                    received-codewords
                    positions)]
               (when-not (every? zero? (syndromes corrected degree))
                 (uncorrectable! :correction-failed-verification {}))
               {:codewords corrected
                :error-count error-count
                :erasure-count erasure-count
                :error-positions positions}))))))))
