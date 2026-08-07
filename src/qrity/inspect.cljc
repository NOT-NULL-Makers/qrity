(ns qrity.inspect
  "A symbol debugger: what a QR code says, and what each module means.

  `symbol-properties` decodes a module matrix and reports everything the
  symbol declares — version, level, mask, block structure, segments with
  their bit ranges, repairs — plus a census of module roles.
  `explain-module` answers the pointed question: given one coordinate, is
  it function pattern, metadata, or data — and if data, exactly which bit
  of which codeword in which block it is, whether the mask flips it, and
  which field of which segment that bit lands in. `describe-symbol` and
  `describe-module` render the same answers as human-readable text.

  Everything is derived from the encoder's own placement machinery
  (`qrity.matrix` coordinates and masks, `qrity.decode` deinterleaving),
  so the explanations cannot drift from what encoding and decoding
  actually do."
  (:require [clojure.string :as string]
            [qrity.decode :as decode]
            [qrity.matrix :as matrix]
            [qrity.parameters :as parameters]
            [qrity.segment :as segment]))

(defn- fail!
  [error message data]
  (throw (ex-info message (assoc data :qrity/error error))))

;; ---------------------------------------------------------------------------
;; Geometric roles

(defn- position-of
  [coordinates coordinate]
  (first (keep-indexed (fn [index candidate]
                         (when (= candidate coordinate) index))
                       coordinates)))

(defn- finder-role
  [dimension row column]
  (let [far (- dimension 4)]
    (some (fn [[corner center-row center-column]]
            (let [distance (max (abs (- row center-row))
                                (abs (- column center-column)))]
              (cond
                (<= distance 1) {:role :finder-pattern
                                 :corner corner
                                 :element (if (zero? distance)
                                            :center
                                            :core)}
                (= distance 2) {:role :finder-pattern
                                :corner corner
                                :element :light-ring}
                (= distance 3) {:role :finder-pattern
                                :corner corner
                                :element :outer-ring}
                (= distance 4) {:role :separator :corner corner}
                :else nil)))
          [[:top-left 3 3]
           [:top-right 3 far]
           [:bottom-left far 3]])))

(defn- format-field
  [bit-index]
  (cond
    (<= bit-index 1) :error-correction-level
    (<= bit-index 4) :mask-reference
    :else :bch-remainder))

(defn- format-role
  [dimension coordinate]
  (if-let [primary-position (position-of matrix/primary-format-coordinates
                                         coordinate)]
    {:role :format-information
     :copy :primary
     :bit-index primary-position
     :field (format-field primary-position)}
    (when-let [secondary-position
               (position-of
                (matrix/secondary-format-coordinates-for dimension)
                coordinate)]
      (let [bit-index (- 14 secondary-position)]
        {:role :format-information
         :copy :secondary
         :bit-index bit-index
         :field (format-field bit-index)}))))

(defn- version-information-role
  [dimension version coordinate]
  (when (<= 7 version)
    (let [{:keys [top-right bottom-left]}
          (matrix/version-information-placement-coordinates dimension)]
      (some (fn [[copy coordinates]]
              (when-let [position (position-of coordinates coordinate)]
                {:role :version-information
                 :copy copy
                 :bit-index (- 17 position)}))
            [[:top-right top-right]
             [:bottom-left bottom-left]]))))

(defn- alignment-role
  [dimension version row column]
  (when (<= 2 version)
    (let [axes (:alignment-pattern-centers
                (parameters/ordinary-qr-parameters version :l))
          last-center (- dimension 7)
          finder-overlaps #{[6 6] [6 last-center] [last-center 6]}]
      (some (fn [[center-row center-column :as center]]
              (let [distance (max (abs (- row center-row))
                                  (abs (- column center-column)))]
                (when (<= distance 2)
                  {:role :alignment-pattern
                   :center center
                   :element (case distance
                              0 :center
                              1 :light-ring
                              2 :outer-ring)})))
            (remove finder-overlaps
                    (for [center-row axes
                          center-column axes]
                      [center-row center-column]))))))

(defn- timing-role
  [row column]
  (cond
    (= 6 row) {:role :timing-pattern :axis :horizontal}
    (= 6 column) {:role :timing-pattern :axis :vertical}
    :else nil))

;; ---------------------------------------------------------------------------
;; Data-region roles

(defn- block-lengths
  [block-groups]
  (into []
        (mapcat (fn [{:keys [block-count data-codeword-count-per-block]}]
                  (repeat block-count data-codeword-count-per-block)))
        block-groups))

(defn- codeword-lookup
  "message codeword index → block index, position, and data/parity role."
  [lengths parity-count]
  (let [data-codeword-count (reduce + lengths)
        block-count (count lengths)
        entries (fn [index-blocks codeword-role]
                  (for [[block-index indexes]
                        (map-indexed vector index-blocks)
                        [position message-index]
                        (map-indexed vector indexes)]
                    [message-index {:block-index block-index
                                    :position-in-block position
                                    :codeword-role codeword-role}]))]
    (into {}
          (concat
           (entries (decode/deinterleave-codewords
                     (vec (range data-codeword-count))
                     lengths)
                    :data)
           (entries (decode/deinterleave-codewords
                     (mapv #(+ data-codeword-count %)
                           (range (* parity-count block-count)))
                     (vec (repeat block-count parity-count)))
                    :error-correction)))))

(defn- stream-field
  "Which message-stream field a data bit offset lands in."
  [decoded version stream-bit-offset]
  (or (some (fn [{:keys [bit-range designator]}]
              (let [[start end] bit-range]
                (when (and (<= start stream-bit-offset)
                           (< stream-bit-offset end))
                  {:field :eci-header :eci-designator designator})))
            (:eci-headers decoded))
      (first
       (keep-indexed
        (fn [segment-index {:keys [bit-range mode]}]
          (let [[start end] bit-range]
            (when (and (<= start stream-bit-offset)
                       (< stream-bit-offset end))
              (let [offset (- stream-bit-offset start)
                    count-width
                    (case mode
                      :numeric (segment/character-count-bit-width version)
                      :alphanumeric
                      (segment/alphanumeric-character-count-bit-width
                       version)
                      :byte (segment/byte-character-count-bit-width
                             version))]
                {:field (cond
                          (< offset 4) :mode-indicator
                          (< offset (+ 4 count-width)) :character-count
                          :else :payload)
                 :segment-index segment-index
                 :segment-mode mode}))))
        (:segments decoded)))
      {:field :terminator-or-padding}))

(defn- data-region-role
  [decoded bit-matrix placement-index row column]
  (let [{:keys [version error-correction-level mask-reference]} decoded
        profile (parameters/ordinary-qr-parameters
                 version error-correction-level)
        total-codeword-count (:total-codeword-count profile)
        message-index (quot placement-index 8)
        bit-index (mod placement-index 8)]
    (if (>= message-index total-codeword-count)
      {:role :remainder-bit
       :placement-index placement-index}
      (let [lengths (block-lengths (:block-groups profile))
            parity-count (:error-correction-codeword-count-per-block
                          profile)
            codeword (merge {:message-index message-index
                             :bit-index bit-index}
                            (get (codeword-lookup lengths parity-count)
                                 message-index))
            module-value (get-in bit-matrix [row column])
            mask-flips? (matrix/data-mask-condition?
                         mask-reference row column)
            data-bit (when module-value
                       (if mask-flips? (- 1 module-value) module-value))
            stream
            (when (= :data (:codeword-role codeword))
              (let [block-offsets (vec (reductions + 0 lengths))
                    stream-codeword (+ (nth block-offsets
                                            (:block-index codeword))
                                       (:position-in-block codeword))
                    stream-bit-offset (+ (* 8 stream-codeword) bit-index)]
                (assoc (stream-field decoded version stream-bit-offset)
                       :stream-bit-offset stream-bit-offset)))]
        (cond-> {:role :data-region
                 :placement-index placement-index
                 :module-value module-value
                 :mask-flips? mask-flips?
                 :data-bit data-bit
                 :codeword codeword}
          stream (assoc :stream stream))))))

;; ---------------------------------------------------------------------------
;; Public inspection

(defn- classify-function-module
  [dimension version row column]
  (or (finder-role dimension row column)
      (format-role dimension [row column])
      (version-information-role dimension version [row column])
      (when (= [row column] [(- dimension 8) 8])
        {:role :dark-module})
      (alignment-role dimension version row column)
      (timing-role row column)))

(defn- placement-positions
  [version]
  (into {}
        (map-indexed (fn [index coordinate] [coordinate index]))
        (matrix/data-coordinates (matrix/function-matrix version))))

(defn explain-module
  "Explains what one module contributes to the symbol.

  Function modules report their pattern, copy, and bit meaning; data-region
  modules report their placement index, mask behavior, exact codeword
  coordinates (message order, block, position, data or parity), and — for
  data codewords — the message-stream field their bit lands in, down to the
  segment. Decodes the matrix to do so, so a matrix that cannot be decoded
  cannot be explained either."
  [bit-matrix [row column :as coordinate]]
  (let [decoded (decode/decode-matrix bit-matrix)
        dimension (count bit-matrix)
        version (:version decoded)]
    (when-not (and (int? row) (int? column)
                   (< -1 row dimension) (< -1 column dimension))
      (fail! :invalid-coordinate
             "The coordinate lies outside the symbol"
             {:coordinate coordinate :dimension dimension}))
    (assoc
     (or (classify-function-module dimension version row column)
         (when-let [placement-index (get (placement-positions version)
                                         coordinate)]
           (data-region-role decoded bit-matrix
                             placement-index row column))
         {:role :unclassified})
     :coordinate coordinate
     :module-value (get-in bit-matrix coordinate))))

(defn symbol-properties
  "Decodes a matrix and reports the symbol's declared properties.

  Adds the error-correction block structure and a census of module roles
  to the decoder's own result (matrix and octet payloads elided)."
  [bit-matrix]
  (let [decoded (decode/decode-matrix bit-matrix)
        {:keys [version error-correction-level]} decoded
        dimension (count bit-matrix)
        profile (parameters/ordinary-qr-parameters
                 version error-correction-level)
        lengths (block-lengths (:block-groups profile))
        parity-count (:error-correction-codeword-count-per-block profile)
        positions (placement-positions version)
        total-codeword-count (:total-codeword-count profile)
        census
        (frequencies
         (for [row (range dimension)
               column (range dimension)]
           (or (:role (classify-function-module
                       dimension version row column))
               (let [placement-index (get positions [row column])]
                 (cond
                   (nil? placement-index) :unclassified
                   (>= (quot placement-index 8) total-codeword-count)
                   :remainder-bit
                   :else :data-region)))))]
    (-> decoded
        (dissoc :reconstructed-matrix :octets)
        (update :segments
                (fn [segments]
                  (mapv #(dissoc % :octets) segments)))
        (assoc :dimension dimension
               :error-correction
               {:block-count (count lengths)
                :data-codeword-count (reduce + lengths)
                :parity-codewords-per-block parity-count
                :error-capacity-per-block (quot parity-count 2)
                :total-codeword-count total-codeword-count}
               :module-census census))))

;; ---------------------------------------------------------------------------
;; Human-readable rendering

(defn describe-symbol
  "Renders `symbol-properties` as a multi-line report string."
  [bit-matrix]
  (let [{:keys [version dimension error-correction-level mask-reference
                format-hamming-distance corrected-error-count
                corrected-erasure-count eci-designator payload
                segments error-correction module-census]}
        (symbol-properties bit-matrix)]
    (string/join
     "\n"
     (concat
      [(str "Ordinary QR symbol, Version " version
            " (" dimension "×" dimension " modules)")
       (str "Error-correction level "
            (string/upper-case (name error-correction-level))
            ", data mask " mask-reference
            ", format-word distance " format-hamming-distance)
       (str "Message: " (:data-codeword-count error-correction)
            " data + "
            (* (:block-count error-correction)
               (:parity-codewords-per-block error-correction))
            " parity codewords in "
            (:block-count error-correction) " block(s), up to "
            (:error-capacity-per-block error-correction)
            " codeword errors correctable per block")
       (str "Corrected on read: " corrected-error-count " error(s), "
            corrected-erasure-count " erasure(s)")]
      (when eci-designator
        [(str "ECI designator: "
              #?(:clj (format "%06d" eci-designator)
                 :cljs (let [digits (str eci-designator)]
                         (str (apply str
                                     (repeat (- 6 (count digits)) "0"))
                              digits))))])
      [(str "Payload: " (pr-str payload))
       "Segments:"]
      (map-indexed
       (fn [index {:keys [mode character-count payload bit-range]}]
         (str "  " (inc index) ". " (name mode)
              ", " character-count " character(s), bits "
              (first bit-range) "–" (dec (second bit-range))
              ": " (pr-str payload)))
       segments)
      ["Module census:"]
      (map (fn [[role occurrences]]
             (str "  " (name role) ": " occurrences))
           (sort-by (comp - val) module-census))))))

(defn describe-module
  "Renders `explain-module` as a short explanation string."
  [bit-matrix coordinate]
  (let [{:keys [role module-value] :as explanation}
        (explain-module bit-matrix coordinate)
        header (str "Module " coordinate " ("
                    (case module-value 1 "dark" 0 "light" "unknown")
                    "): ")]
    (str
     header
     (case role
       :finder-pattern (str (name (:element explanation)) " of the "
                            (name (:corner explanation))
                            " finder pattern")
       :separator (str (name (:corner explanation))
                       " finder-pattern separator")
       :timing-pattern (str (name (:axis explanation)) " timing pattern")
       :alignment-pattern (str (name (:element explanation))
                               " of the alignment pattern centered at "
                               (:center explanation))
       :format-information (str "format-information bit "
                                (:bit-index explanation) " ("
                                (name (:field explanation)) ", "
                                (name (:copy explanation)) " copy)")
       :version-information (str "version-information bit "
                                 (:bit-index explanation) " ("
                                 (name (:copy explanation)) " copy)")
       :dark-module "the fixed dark module"
       :remainder-bit (str "remainder bit (placement index "
                           (:placement-index explanation) ")")
       :data-region
       (let [{:keys [codeword stream data-bit mask-flips?]} explanation]
         (str "data-region bit " (:bit-index codeword)
              " of message codeword " (:message-index codeword)
              " — " (name (:codeword-role codeword))
              " codeword, block " (:block-index codeword)
              " position " (:position-in-block codeword)
              (if mask-flips?
                (str "; mask flips it to data bit " data-bit)
                (str "; unmasked data bit " data-bit))
              (when stream
                (str "; stream bit " (:stream-bit-offset stream) " → "
                     (name (:field stream))
                     (when (:segment-index stream)
                       (str " of segment "
                            (inc (:segment-index stream)) " ("
                            (name (:segment-mode stream)) ")"))))))
       "unclassified"))))
