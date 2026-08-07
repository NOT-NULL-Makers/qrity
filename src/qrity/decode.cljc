(ns qrity.decode
  "Exploratory pure decoding of a resolved ordinary-QR module matrix.

  This namespace inverts the Clause 7.1 encoding stages for the encoder's own
  supported subset: single-segment Numeric, Alphanumeric, and default-ECI Byte
  symbols, Versions 1-40, levels L/M/Q/H. It reuses the encoder's tables and
  transforms wherever the standard makes a stage self-inverse or replayable:
  the canonical function templates and placement traversal, the Table 10 masks,
  the 32 possible format words, the block-interleaving order, and Reed-Solomon
  parity generation.

  Damage tolerance is realized in codeword space, not pixel space:
  Reed-Solomon correction repairs up to ⌊parity/2⌋ damaged codewords per
  block, and format information tolerates up to three flipped modules per
  copy because exhaustive candidate matching realizes the BCH(15,5)
  correction capacity. A successfully decoded symbol also yields
  `:reconstructed-matrix` — the pristine module matrix re-encoded from the
  corrected codewords, which is what \"repairing the image\" means here: the
  damaged pixels are diagnosis material, and the repaired symbol is a
  re-render. Message structure is verified by re-encoding the parsed segment
  with the encoder's own termination and padding; anything the encoder could
  not have produced is refused rather than half-read.

  See docs/decoding-exploration.md for the pipeline this fits into."
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.matrix :as matrix]
            [qrity.message :as message]
            [qrity.metadata :as metadata]
            [qrity.parameters :as parameters]
            [qrity.reed-solomon :as reed-solomon]
            [qrity.render :as render]
            [qrity.segment :as segment]))

(def format-information-tolerance
  "Maximum tolerated Hamming distance to a valid format word.

  BCH(15,5) codewords are pairwise at distance seven or more, so within this
  radius at most one candidate can match a given copy."
  3)

(defn- fail!
  [error message data clause]
  (throw
   (ex-info message (assoc data :qrity/error error :clause clause))))

;; ---------------------------------------------------------------------------
;; Format information (Clause 7.9)

(def ^:private format-candidates
  "All 32 valid format words, paired with the parameters they encode."
  (vec
   (for [error-correction-level parameters/error-correction-levels
         mask-reference (range 8)]
     {:error-correction-level error-correction-level
      :mask-reference mask-reference
      :format-bits (metadata/format-information-bits
                    error-correction-level
                    mask-reference)})))

(defn- hamming-distance
  [bits-a bits-b]
  (count (filter true? (map not= bits-a bits-b))))

(defn- secondary-format-coordinates-for
  "The dimension-dependent second format copy, in the encoder's write order.

  The encoder writes this copy least-significant bit first; reading these
  coordinates therefore yields the reversed format word."
  [dimension]
  (into []
        (concat
         (for [column (range (dec dimension) (- dimension 9) -1)]
           [8 column])
         (for [row (range (- dimension 7) dimension)]
           [row 8]))))

(defn- read-modules
  [matrix coordinates]
  (mapv #(get-in matrix %) coordinates))

(defn- nearest-format-candidate
  [format-bits]
  (apply min-key
         :hamming-distance
         (map #(assoc % :hamming-distance
                      (hamming-distance format-bits (:format-bits %)))
              format-candidates)))

(defn read-format-information
  "Recovers level and mask from a 0/1 matrix's two format-information copies.

  Each copy is matched independently against the 32 valid format words and the
  closer match wins, so up to `format-information-tolerance` damaged modules
  per copy are corrected. Disagreeing equally-damaged copies are refused as
  ambiguous rather than guessed."
  [bit-matrix]
  (let [dimension (count bit-matrix)
        primary (nearest-format-candidate
                 (read-modules bit-matrix
                               matrix/primary-format-coordinates))
        secondary (nearest-format-candidate
                   (vec
                    (rseq
                     (read-modules
                      bit-matrix
                      (secondary-format-coordinates-for dimension)))))
        best (min-key :hamming-distance primary secondary)]
    (when (> (:hamming-distance best) format-information-tolerance)
      (fail! :unreadable-format-information
             "No valid format word within the correctable distance"
             {:primary-hamming-distance (:hamming-distance primary)
              :secondary-hamming-distance (:hamming-distance secondary)
              :tolerance format-information-tolerance}
             "7.9"))
    (when (and (= (:hamming-distance primary) (:hamming-distance secondary))
               (not= (dissoc primary :hamming-distance :format-bits)
                     (dissoc secondary :hamming-distance :format-bits)))
      (fail! :ambiguous-format-information
             "The two format-information copies decode to different parameters"
             {:primary (dissoc primary :format-bits)
              :secondary (dissoc secondary :format-bits)}
             "7.9"))
    (select-keys best
                 [:error-correction-level
                  :mask-reference
                  :hamming-distance])))

;; ---------------------------------------------------------------------------
;; Unmasking and codeword extraction (Clauses 7.7.3 and 7.8)

(defn- construction-matrix
  "Overlays 0/1 data modules onto the canonical function template.

  Reserved metadata and function modules keep their template cells, so the
  result is exactly the encoder's metadata-ready shape and the encoder's own
  mask transform applies unchanged. An unknown (nil) module gets a light
  placeholder; its coordinate is separately reported as an erasure and the
  guess never survives uncorrected."
  [bit-matrix template]
  (mapv (fn [bit-row template-row]
          (mapv (fn [bit template-cell]
                  (if (= :unset template-cell)
                    (if (= 1 bit) :dark :light)
                    template-cell))
                bit-row
                template-row))
        bit-matrix
        template))

(defn- extract-message
  "Unmasks the encoding region and reads it back in placement order.

  Returns the message codewords together with the sorted indexes of
  codewords touched by unknown (nil) modules — the erasures."
  [bit-matrix version mask-reference total-codeword-count]
  (let [template (matrix/function-matrix version)
        unmasked (matrix/apply-data-mask
                  (construction-matrix bit-matrix template)
                  mask-reference)
        coordinates (matrix/data-coordinates template)
        message-bits (mapv #(if (= :dark (get-in unmasked %)) 1 0)
                           coordinates)
        message-bit-count (* 8 total-codeword-count)]
    ;; Remainder bits carry no message content and are ignored when present.
    {:message-codewords (bits/bits->codewords
                         (subvec message-bits 0 message-bit-count))
     :erased-codeword-indexes
     (into (sorted-set)
           (keep-indexed
            (fn [bit-index coordinate]
              (when (and (< bit-index message-bit-count)
                         (nil? (get-in bit-matrix coordinate)))
                (quot bit-index 8))))
           coordinates)}))

;; ---------------------------------------------------------------------------
;; Deinterleaving and parity verification (Clauses 7.6 and 7.5.2)

(defn- block-lengths
  [block-groups]
  (into []
        (mapcat
         (fn [{:keys [block-count data-codeword-count-per-block]}]
           (repeat block-count data-codeword-count-per-block)))
        block-groups))

(defn- deinterleave-codewords
  "Redistributes column-interleaved codewords into their original blocks."
  [codewords lengths]
  (let [placement-order (for [codeword-index (range (apply max lengths))
                              [block-index length] (map-indexed vector lengths)
                              :when (< codeword-index length)]
                          block-index)]
    (reduce (fn [blocks [block-index codeword]]
              (update blocks block-index conj codeword))
            (vec (repeat (count lengths) []))
            (map vector placement-order codewords))))

(defn- block-erasure-positions
  "Maps erased interleaved codeword indexes onto per-block positions.

  Replaying the deinterleave over index vectors yields, for each block, the
  interleaved index of every one of its codewords; membership in the erased
  set then names the erased positions inside the block, data and parity
  alike."
  [erased-codeword-indexes lengths parity-count data-codeword-count]
  (let [data-index-blocks
        (deinterleave-codewords (vec (range data-codeword-count)) lengths)
        parity-index-blocks
        (deinterleave-codewords
         (mapv #(+ data-codeword-count %)
               (range (* parity-count (count lengths))))
         (vec (repeat (count lengths) parity-count)))]
    (mapv (fn [data-indexes parity-indexes block-length]
            (into []
                  (concat
                   (keep-indexed
                    (fn [position interleaved-index]
                      (when (contains? erased-codeword-indexes
                                       interleaved-index)
                        position))
                    data-indexes)
                   (keep-indexed
                    (fn [position interleaved-index]
                      (when (contains? erased-codeword-indexes
                                       interleaved-index)
                        (+ block-length position)))
                    parity-indexes))))
          data-index-blocks
          parity-index-blocks
          lengths)))

(defn- correct-blocks
  "Reed-Solomon-corrects each received block, or refuses the whole message.

  Returns the corrected data blocks with the total corrected error and
  erasure counts. A block whose damage exceeds the parity's correction
  capacity makes the message uncorrectable; the failing blocks are named in
  the error."
  [data-blocks error-correction-blocks parity-count erasures-per-block]
  (let [corrections
        (mapv (fn [data-block parity-block erasure-positions]
                (try
                  (reed-solomon/correct-codewords
                   (into data-block parity-block)
                   parity-count
                   erasure-positions)
                  (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default)
                         error
                    (when-not (ex-data error) (throw error))
                    nil)))
              data-blocks
              error-correction-blocks
              erasures-per-block)
        uncorrectable-blocks
        (into []
              (keep-indexed (fn [block-index correction]
                              (when-not correction block-index)))
              corrections)]
    (when (seq uncorrectable-blocks)
      (fail! :uncorrectable-message
             "At least one block carries more errors than its parity corrects"
             {:reason :error-capacity-exceeded
              :uncorrectable-block-indexes uncorrectable-blocks
              :error-capacity-per-block (quot parity-count 2)
              :block-count (count data-blocks)}
             "7.5.2"))
    {:data-blocks (mapv (fn [data-block {:keys [codewords]}]
                          (subvec codewords 0 (count data-block)))
                        data-blocks
                        corrections)
     :corrected-error-count (transduce (map :error-count) + corrections)
     :corrected-erasure-count (transduce (map :erasure-count)
                                         + corrections)}))

;; ---------------------------------------------------------------------------
;; Bit-stream parsing (Clause 7.4)

(def ^:private mode-indicator-values
  {1 :numeric
   2 :alphanumeric
   4 :byte})

(defn- read-unsigned-integer
  [data-bits offset width failure-context]
  (when (> (+ offset width) (count data-bits))
    (fail! :truncated-bit-stream
           "The data bit stream ends inside a segment field"
           (assoc failure-context :offset offset :width width)
           "7.4"))
  (bits/bits->unsigned-integer
   (subvec data-bits offset (+ offset width))))

(defn- zero-padded-digits
  [value digit-count]
  (let [digits (str value)]
    (str (apply str (repeat (- digit-count (count digits)) "0")) digits)))

(defn- read-numeric-group
  [data-bits offset digit-count]
  (let [width (case digit-count 1 4 2 7 3 10)
        maximum (dec (long (Math/pow 10 digit-count)))
        value (read-unsigned-integer data-bits offset width
                                     {:mode :numeric})]
    (when (> value maximum)
      (fail! :invalid-numeric-group
             "A Numeric-mode group exceeds its digit range"
             {:mode :numeric :value value :digit-count digit-count}
             "7.4.3"))
    (zero-padded-digits value digit-count)))

(defn- parse-numeric-payload
  [data-bits offset character-count]
  (let [group-sizes (concat (repeat (quot character-count 3) 3)
                            (when (pos? (rem character-count 3))
                              [(rem character-count 3)]))]
    (loop [offset offset
           group-sizes group-sizes
           payload ""]
      (if-let [digit-count (first group-sizes)]
        (recur (+ offset (case digit-count 1 4 2 7 3 10))
               (next group-sizes)
               (str payload
                    (read-numeric-group data-bits offset digit-count)))
        {:payload payload :end-offset offset}))))

(defn- alphanumeric-character
  [value failure-context]
  (when (>= value 45)
    (fail! :invalid-alphanumeric-value
           "An Alphanumeric-mode value falls outside Table 5"
           (assoc failure-context :value value)
           "7.4.4"))
  (nth bits/alphanumeric-repertoire value))

(defn- parse-alphanumeric-payload
  [data-bits offset character-count]
  (let [pair-count (quot character-count 2)
        pairs-end (+ offset (* 11 pair-count))
        pairs (map (fn [pair-offset]
                     (let [value (read-unsigned-integer
                                  data-bits pair-offset 11
                                  {:mode :alphanumeric})]
                       (str (alphanumeric-character
                             (quot value 45) {:mode :alphanumeric})
                            (alphanumeric-character
                             (rem value 45) {:mode :alphanumeric}))))
                   (range offset pairs-end 11))
        final (when (odd? character-count)
                (alphanumeric-character
                 (read-unsigned-integer data-bits pairs-end 6
                                        {:mode :alphanumeric})
                 {:mode :alphanumeric}))]
    {:payload (apply str (concat pairs (when final [final])))
     :end-offset (if final (+ pairs-end 6) pairs-end)}))

(defn- parse-byte-payload
  [data-bits offset character-count]
  (let [octets (mapv (fn [octet-offset]
                       (read-unsigned-integer data-bits octet-offset 8
                                              {:mode :byte}))
                     (range offset (+ offset (* 8 character-count)) 8))]
    {:payload (apply str (map char octets))
     :octets octets
     :end-offset (+ offset (* 8 character-count))}))

(defn- character-count-width
  [mode version]
  (case mode
    :numeric (segment/character-count-bit-width version)
    :alphanumeric (segment/alphanumeric-character-count-bit-width version)
    :byte (segment/byte-character-count-bit-width version)))

(defn- verify-message-structure!
  "Requires the received data codewords to equal the parsed segment re-encoded.

  Reusing `bits/pad-data-codewords` makes this check exact: termination,
  byte alignment, and pad codewords must all match what the encoder itself
  would emit. Multi-segment or foreign-padding symbols are refused here."
  [data-codewords segment-bits]
  (when-not (= data-codewords
               (bits/pad-data-codewords segment-bits
                                        (count data-codewords)))
    (fail! :unsupported-message-structure
           "The bit stream is not a single canonically padded segment"
           {:reason :not-single-canonical-segment
            :segment-bit-count (count segment-bits)
            :data-codeword-count (count data-codewords)}
           "7.4")))

(defn- parse-data-codewords
  [data-codewords version]
  (let [data-bits (bits/codewords->bits data-codewords)
        mode-value (read-unsigned-integer data-bits 0 4 {})
        mode (or (mode-indicator-values mode-value)
                 (fail! :unsupported-mode
                        "The mode indicator is outside the supported subset"
                        {:mode-indicator-value mode-value
                         :supported-modes (set (vals mode-indicator-values))}
                        "7.4.1"))
        count-width (character-count-width mode version)
        character-count (read-unsigned-integer data-bits 4 count-width
                                               {:mode mode})
        payload-offset (+ 4 count-width)]
    (when (zero? character-count)
      (fail! :empty-segment
             "The segment declares zero characters"
             {:mode mode}
             "7.4.2"))
    (let [{:keys [payload octets end-offset]}
          (case mode
            :numeric (parse-numeric-payload
                      data-bits payload-offset character-count)
            :alphanumeric (parse-alphanumeric-payload
                           data-bits payload-offset character-count)
            :byte (parse-byte-payload
                   data-bits payload-offset character-count))]
      (verify-message-structure! data-codewords
                                 (subvec data-bits 0 end-offset))
      (cond-> {:mode mode
               :character-count character-count
               :payload payload}
        octets (assoc :octets octets)))))

;; ---------------------------------------------------------------------------
;; Public entry point

(defn- decodable-matrix?
  "A square module matrix whose cells are 0, 1, or nil for unknown."
  [value]
  (and (vector? value)
       (pos? (count value))
       (every? (fn [row]
                 (and (vector? row)
                      (= (count value) (count row))
                      (every? #(or (nil? %) (= 0 %) (= 1 %)) row)))
               value)))

(defn- version-for-dimension
  [dimension]
  (let [version (quot (- dimension 17) 4)]
    (when-not (and (<= 21 dimension 177)
                   (zero? (mod (- dimension 17) 4)))
      (fail! :invalid-symbol-dimension
             "The matrix dimension matches no ordinary QR version"
             {:dimension dimension}
             "6.3.1"))
    version))

(defn- reconstruct-matrix
  "Re-encodes corrected data codewords into the pristine module matrix.

  Because the version, level, and mask are all recovered during decoding,
  the encoder's own stages rebuild the exact matrix the original encoder
  produced — this is the repaired symbol, generated rather than patched."
  [data-codewords version error-correction-level mask-reference]
  (let [{:keys [message-bits]}
        (message/construct-final-message
         data-codewords version error-correction-level)
        placed (:matrix (matrix/place-data
                         (matrix/function-matrix version)
                         message-bits))]
    (matrix/final-bit-matrix
     (matrix/resolve-metadata
      (matrix/apply-data-mask placed mask-reference)
      error-correction-level
      mask-reference))))

(defn decode-matrix
  "Decodes a resolved 0/1 ordinary-QR module matrix back to its payload.

  Returns the recovered parameters and payload:

      {:version 2
       :error-correction-level :m
       :mask-reference 3
       :format-hamming-distance 0
       :corrected-error-count 0
       :mode :numeric
       :character-count 35
       :payload \"867...\"
       :reconstructed-matrix [[...]]}

  A module may be nil, meaning its value is unknown — a sample that fell
  outside the picture, or a region a caller knows is covered. Unknown
  modules become Reed-Solomon erasures, which cost half of what errors
  cost: per block, 2·errors + erasures may not exceed the parity count.
  Repairs are reported in `:corrected-error-count` and
  `:corrected-erasure-count`, and `:reconstructed-matrix` is the pristine
  symbol re-encoded from the corrected codewords. Byte-mode results
  additionally carry `:octets`; `:payload` is then the ISO/IEC 8859-1
  reading of those octets. Failures are structured `ex-info` values."
  [bit-matrix]
  (when-not (decodable-matrix? bit-matrix)
    (fail! :invalid-matrix
           "Decoding requires a square matrix of 0, 1, or nil (unknown) modules"
           {:matrix bit-matrix}
           "6.3.1"))
  (let [version (version-for-dimension (count bit-matrix))
        {:keys [error-correction-level mask-reference hamming-distance]}
        (read-format-information bit-matrix)
        profile (parameters/ordinary-qr-parameters
                 version
                 error-correction-level)
        {:keys [message-codewords erased-codeword-indexes]}
        (extract-message
         bit-matrix
         version
         mask-reference
         (:total-codeword-count profile))
        lengths (block-lengths (:block-groups profile))
        data-codeword-count (reduce + lengths)
        data-blocks (deinterleave-codewords
                     (subvec message-codewords 0 data-codeword-count)
                     lengths)
        parity-count (:error-correction-codeword-count-per-block profile)
        error-correction-blocks (deinterleave-codewords
                                 (subvec message-codewords
                                         data-codeword-count)
                                 (vec (repeat (count lengths)
                                              parity-count)))
        {corrected-blocks :data-blocks
         :keys [corrected-error-count corrected-erasure-count]}
        (correct-blocks data-blocks
                        error-correction-blocks
                        parity-count
                        (block-erasure-positions
                         erased-codeword-indexes
                         lengths
                         parity-count
                         data-codeword-count))
        data-codewords (into [] (mapcat identity) corrected-blocks)]
    (merge {:version version
            :error-correction-level error-correction-level
            :mask-reference mask-reference
            :format-hamming-distance hamming-distance
            :corrected-error-count corrected-error-count
            :corrected-erasure-count corrected-erasure-count
            :reconstructed-matrix (reconstruct-matrix
                                   data-codewords
                                   version
                                   error-correction-level
                                   mask-reference)}
           (parse-data-codewords data-codewords version))))

(s/def ::format-hamming-distance
  (s/int-in 0 (inc format-information-tolerance)))
(s/def ::mode #{:numeric :alphanumeric :byte})
(s/def ::character-count pos-int?)
(s/def ::payload (s/and string? seq))
(s/def ::corrected-error-count nat-int?)
(s/def ::corrected-erasure-count nat-int?)
(s/def ::reconstructed-matrix ::render/binary-square-matrix)

(s/def ::decoded-symbol
  (s/keys :req-un [::parameters/version
                   ::parameters/error-correction-level
                   ::parameters/mask-reference
                   ::format-hamming-distance
                   ::corrected-error-count
                   ::corrected-erasure-count
                   ::reconstructed-matrix
                   ::mode
                   ::character-count
                   ::payload]
          :opt-un [::bits/octets]))

(s/def ::decodable-matrix decodable-matrix?)

(s/fdef decode-matrix
  :args (s/cat :bit-matrix ::decodable-matrix)
  :ret ::decoded-symbol)
