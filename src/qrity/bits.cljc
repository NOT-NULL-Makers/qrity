(ns qrity.bits
  "Pure bit, codeword, and Clause 7.4 payload-packing primitives."
  (:require [clojure.spec.alpha :as s]))

;; ISO/IEC 18004:2015, Clause 7.4.4, Table 5. Position is character value.
(def alphanumeric-repertoire
  "Ordered ordinary-QR Alphanumeric repertoire in Table 5 value order."
  "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ $%*+-./:")

(def ^:private alphanumeric-character-values
  (zipmap alphanumeric-repertoire (range)))

(defn- fail!
  [error message data clause]
  (throw
   (ex-info
    message
    (assoc data :qrity/error error :clause clause))))

(defn- validate-alphanumeric-payload!
  [payload]
  (when-not (string? payload)
    (fail! :invalid-alphanumeric-payload
           "Alphanumeric payload must be a non-empty Table 5 string"
           {:mode :alphanumeric
            :payload payload
            :reason :non-string-payload}
           "7.4.4"))
  (when (empty? payload)
    (fail! :invalid-alphanumeric-payload
           "Alphanumeric payload must be a non-empty Table 5 string"
           {:mode :alphanumeric
            :payload payload
            :reason :empty-payload}
           "7.4.4"))
  (when-let [[index character]
             (first
              (keep-indexed
               (fn [index character]
                 (when-not (contains?
                            alphanumeric-character-values
                            character)
                   [index character]))
               payload))]
    (fail! :invalid-alphanumeric-payload
           "Alphanumeric payload contains a character outside Table 5"
           {:mode :alphanumeric
            :payload payload
            :reason :non-alphanumeric-character
            :character-index index
            :character (str character)}
           "7.4.4")))

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

(defn- validate-octets!
  [octets]
  (when-not (vector? octets)
    (fail! :invalid-byte-payload
           "Byte payload must be a non-empty vector of octets"
           {:mode :byte
            :octets octets
            :reason :non-vector-payload}
           "7.4.5"))
  (when (empty? octets)
    (fail! :invalid-byte-payload
           "Byte payload must be a non-empty vector of octets"
           {:mode :byte
            :octets octets
            :reason :empty-payload}
           "7.4.5"))
  (when-let [[index value]
             (first
              (keep-indexed
               (fn [index value]
                 (when-not (and (int? value) (<= 0 value 255))
                   [index value]))
               octets))]
    (fail! :invalid-byte-payload
           "Byte payload contains a value outside the octet range"
           {:mode :byte
            :octets octets
            :reason :non-octet
            :octet-index index
            :value value}
           "7.4.5")))

(defn iso-8859-1-string->octets
  "Maps a non-empty ISO/IEC 8859-1 string to equal-valued octets.

  The QR default interpretation is ECI 000003. Clojure and ClojureScript
  strings are inspected as UTF-16 code units so BMP values above `U+00FF`,
  supplementary characters, and lone surrogates fail identically on both
  runtimes. This function does not emit an ECI header."
  [text]
  (when-not (string? text)
    (fail! :invalid-iso-8859-1-text
           "ISO/IEC 8859-1 text must be a non-empty string"
           {:mode :byte
            :text text
            :reason :non-string-text}
           "6.1, 7.3.2, 7.4.5"))
  (when (empty? text)
    (fail! :invalid-iso-8859-1-text
           "ISO/IEC 8859-1 text must be a non-empty string"
           {:mode :byte
            :text text
            :reason :empty-payload}
           "6.1, 7.3.2, 7.4.5"))
  (mapv
   (fn [code-unit-index]
     (let [code-unit
           (character-code (.charAt text code-unit-index))]
       (when (> code-unit 0xFF)
         (fail!
          :invalid-iso-8859-1-text
          "Text contains a UTF-16 code unit outside ISO/IEC 8859-1"
          {:mode :byte
           :text text
           :reason :non-iso-8859-1-code-unit
           :code-unit-index code-unit-index
           :code-unit code-unit}
          "6.1, 7.3.2, 7.4.5"))
       code-unit))
   (range (count text))))

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

(defn alphanumeric-data-bits
  "Encodes a non-empty Table 5 string under Clause 7.4.4.

  Each pair is encoded as `45 × first + second` in 11 bits. A final
  unpaired character is encoded in 6 bits. Mode and character-count
  indicators, termination, and padding are deliberately not included."
  [payload]
  (validate-alphanumeric-payload! payload)
  (into []
        (mapcat
         (fn [group]
           (let [first-value
                 (alphanumeric-character-values (first group))
                 pair? (= 2 (count group))
                 value
                 (if pair?
                   (+ (* 45 first-value)
                      (alphanumeric-character-values (second group)))
                   first-value)]
             (unsigned-integer->bits value (if pair? 11 6)))))
        (partition-all 2 payload)))

(defn byte-data-bits
  "Encodes a non-empty vector of octets under Clause 7.4.5.

  Every octet becomes its eight most-significant-bit-first bits. Mode and
  octet-count indicators, termination, and padding are not included."
  [octets]
  (validate-octets! octets)
  (codewords->bits octets))

(defn numeric-segment-bits
  "Builds an ordinary-QR Numeric segment without terminator or padding."
  [digits character-count-width]
  (into [0 0 0 1]
        (concat
         (unsigned-integer->bits (count digits) character-count-width)
         (numeric-data-bits digits))))

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

(s/def ::alphanumeric-payload
  (s/and
   string?
   seq
   #(every? alphanumeric-character-values %)))

(s/def ::alphanumeric-data-bits
  (s/coll-of #{0 1} :kind vector? :min-count 6))

(s/def ::octets
  (s/coll-of #(and (int? %) (<= 0 % 255))
             :kind vector?
             :min-count 1))

(s/def ::byte-data-bits
  (s/coll-of #{0 1} :kind vector? :min-count 8))

(s/def ::iso-8859-1-text
  (s/and
   string?
   seq
   (fn [text]
     (every?
      #(<= (character-code (.charAt text %)) 0xFF)
      (range (count text))))))

(s/fdef alphanumeric-data-bits
  :args (s/cat :payload ::alphanumeric-payload)
  :ret ::alphanumeric-data-bits)

(s/fdef byte-data-bits
  :args (s/cat :octets ::octets)
  :ret ::byte-data-bits)

(s/fdef iso-8859-1-string->octets
  :args (s/cat :text ::iso-8859-1-text)
  :ret ::octets)
