(ns qrity.bits)

(defn unsigned-integer->bits
  "Returns `width` most-significant-bit-first bits for a non-negative integer."
  [value width]
  (when-not (and (int? value)
                 (int? width)
                 (<= 0 value)
                 (<= 0 width)
                 (< value (bit-shift-left 1 width)))
    (throw (ex-info "Integer does not fit requested bit width"
                    {:value value :width width})))
  (mapv (fn [shift]
          (bit-and 1 (bit-shift-right value shift)))
        (range (dec width) -1 -1)))

(defn bits->unsigned-integer
  "Converts a most-significant-bit-first sequence to an unsigned integer."
  [bits]
  (reduce (fn [value bit]
            (+ (* value 2) bit))
          0
          bits))

(defn codewords->bits
  [codewords]
  (into [] (mapcat #(unsigned-integer->bits % 8)) codewords))

(defn bits->codewords
  [bits]
  (when-not (zero? (mod (count bits) 8))
    (throw (ex-info "Bit count is not a whole number of codewords"
                    {:bit-count (count bits)})))
  (mapv bits->unsigned-integer (partition 8 bits)))

(defn- character-code
  [character]
  #?(:clj (int character)
     :cljs (.charCodeAt character 0)))

(defn- decimal-value
  [digits]
  (reduce (fn [value character]
            (+ (* value 10)
               (- (character-code character)
                  (character-code \0))))
          0
          digits))

(defn numeric-data-bits
  "Encodes Numeric-mode digit groups under ISO/IEC 18004:2015, Clause 7.4.3."
  [digits]
  (into []
        (mapcat (fn [group]
                  (let [width (case (count group)
                                1 4
                                2 7
                                3 10)]
                    (unsigned-integer->bits (decimal-value group) width))))
        (partition-all 3 digits)))

(defn numeric-segment-bits
  "Builds an ordinary-QR Numeric segment without terminator or padding.

  The one-argument form preserves the fixed Version 1–9 behavior."
  ([digits]
   (numeric-segment-bits digits 10))
  ([digits character-count-width]
   (into [0 0 0 1]
         (concat
          (unsigned-integer->bits (count digits) character-count-width)
          (numeric-data-bits digits)))))

(defn pad-data-codewords
  "Terminates, byte-aligns, and pads segment bits to `codeword-count`."
  [segment-bits codeword-count]
  (let [capacity-bits (* 8 codeword-count)]
    (when (> (count segment-bits) capacity-bits)
      (throw (ex-info "Segment exceeds data-codeword capacity"
                      {:segment-bit-count (count segment-bits)
                       :capacity-bits capacity-bits})))
    (let [terminator-count
          (min 4 (- capacity-bits (count segment-bits)))
          terminated (into segment-bits (repeat terminator-count 0))
          alignment-count (mod (- 8 (mod (count terminated) 8)) 8)
          aligned (into terminated (repeat alignment-count 0))
          initial-codewords (bits->codewords aligned)
          pad-count (- codeword-count (count initial-codewords))]
      (into initial-codewords
            (take pad-count (cycle [0xEC 0x11]))))))
