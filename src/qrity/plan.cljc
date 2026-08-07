(ns qrity.plan
  "Optimal mode segmentation and automatic version selection.

  Given free text, chooses the cheapest sequence of Numeric, Alphanumeric,
  and Byte segments by dynamic programming over code points — each state is
  a mode plus its position inside the mode's packing group, so the exact
  incremental bit cost of extending or opening a segment is known — and
  then the smallest version whose data capacity fits the planned bits.
  Character-count widths change at Versions 10 and 27, so the plan is made
  per version class and the first class that fits wins.

  Text outside ISO/IEC 8859-1 switches the symbol to ECI 000026 and UTF-8
  byte payloads (costed per code point at their true octet length); text
  within it keeps the QR default interpretation with no ECI header."
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.parameters :as parameters]
            [qrity.segment :as segment]
            [qrity.text :as text]))

(def utf-8-eci-designator 26)

(defn- fail!
  [error message data]
  (throw
   (ex-info message (assoc data :qrity/error error :clause "7.4"))))

(def ^:private alphanumeric-values
  (into #{}
        (map (fn [index]
               #?(:clj (int (.charAt bits/alphanumeric-repertoire index))
                  :cljs (.charCodeAt bits/alphanumeric-repertoire index))))
        (range (count bits/alphanumeric-repertoire))))

(def ^:private mode-phase-counts
  "Positions inside one packing group: 3 digits, 2 characters, 1 octet."
  {:numeric 3 :alphanumeric 2 :byte 1})

(defn- code-point-modes
  [code-point]
  (cond-> [:byte]
    (contains? alphanumeric-values code-point) (conj :alphanumeric)
    (<= 48 code-point 57) (conj :numeric)))

(defn- append-cost
  "Bits added by one more code point at a given position in its group."
  [mode phase octet-count]
  (case mode
    :numeric (nth [4 3 3] phase)
    :alphanumeric (nth [6 5] phase)
    :byte (* 8 octet-count)))

(defn- segment-header-cost
  [mode version]
  (+ 4
     (case mode
       :numeric (segment/character-count-bit-width version)
       :alphanumeric (segment/alphanumeric-character-count-bit-width version)
       :byte (segment/byte-character-count-bit-width version))))

(defn- plan-states
  "One DP layer per code point: cheapest cost to end in [mode phase]."
  [code-points version utf-8?]
  (reduce
   (fn [trace code-point]
     (let [previous-states (peek trace)
           octet-count (if utf-8?
                         (text/code-point-octet-count code-point)
                         1)
           best-previous (when (seq previous-states)
                           (apply min-key (comp :cost val)
                                  previous-states))
           candidates
           (mapcat
            (fn [mode]
              (let [phases (mode-phase-counts mode)]
                (concat
                 (for [phase (range phases)
                       :let [previous (get previous-states [mode phase])]
                       :when previous]
                   [[mode (mod (inc phase) phases)]
                    {:cost (+ (:cost previous)
                              (append-cost mode phase octet-count))
                     :previous [mode phase]
                     :new-segment? false}])
                 [[[mode (mod 1 phases)]
                   {:cost (+ (if best-previous
                               (:cost (val best-previous))
                               0)
                             (segment-header-cost mode version)
                             (append-cost mode 0 octet-count))
                    :previous (when best-previous (key best-previous))
                    :new-segment? true}]])))
            (code-point-modes code-point))]
       (conj trace
             (reduce (fn [states [state-key entry]]
                       (if (< (:cost entry)
                              (get-in states [state-key :cost]
                                      #?(:clj Long/MAX_VALUE
                                         :cljs js/Number.MAX_SAFE_INTEGER)))
                         (assoc states state-key entry)
                         states))
                     {}
                     candidates))))
   [nil]
   code-points))

(defn- backtrack-segments
  "Walks the DP trace backwards into mode segments of code points."
  [trace code-points]
  (let [final-states (peek trace)
        final (apply min-key (comp :cost val) final-states)]
    (loop [index (dec (count code-points))
           state-key (key final)
           current-code-points nil
           segments nil]
      (let [entry (get-in trace [(inc index) state-key])
            current-code-points (cons (nth code-points index)
                                      current-code-points)]
        (if (:new-segment? entry)
          (let [segments (cons {:mode (first state-key)
                                :code-points (vec current-code-points)}
                               segments)]
            (if (zero? index)
              (vec segments)
              (recur (dec index) (:previous entry) nil segments)))
          (recur (dec index) (:previous entry)
                 current-code-points segments))))))

(defn eci-header-bits
  "The Clause 7.4.2 ECI header for a designator, most significant bit first."
  [designator]
  (when-not (and (int? designator) (<= 0 designator 999999))
    (fail! :invalid-eci-designator
           "An ECI designator must be an integer from 0 through 999999"
           {:designator designator}))
  (into [0 1 1 1]
        (cond
          (< designator 128)
          (bits/unsigned-integer->bits designator 8)
          (< designator 16384)
          (into [1 0] (bits/unsigned-integer->bits designator 14))
          :else
          (into [1 1 0] (bits/unsigned-integer->bits designator 21)))))

(defn- segment-octets
  [code-points utf-8?]
  (if utf-8?
    (text/utf-8-octets (text/code-points->string code-points))
    (vec code-points)))

(defn- segment-bit-vector
  [{:keys [mode code-points]} version utf-8?]
  (let [payload (text/code-points->string code-points)]
    (case mode
      :numeric
      (bits/numeric-segment-bits
       payload
       (segment/character-count-bit-width version))

      :alphanumeric
      (into [0 0 1 0]
            (concat
             (bits/unsigned-integer->bits
              (count payload)
              (segment/alphanumeric-character-count-bit-width version))
             (bits/alphanumeric-data-bits payload)))

      :byte
      (let [octets (segment-octets code-points utf-8?)]
        (into [0 1 0 0]
              (concat
               (bits/unsigned-integer->bits
                (count octets)
                (segment/byte-character-count-bit-width version))
               (bits/byte-data-bits octets)))))))

(defn data-codeword-count
  "Data (non-parity) codewords available at a version and level."
  [version error-correction-level]
  (transduce
   (map (fn [{:keys [block-count data-codeword-count-per-block]}]
          (* block-count data-codeword-count-per-block)))
   +
   (:block-groups
    (parameters/ordinary-qr-parameters version error-correction-level))))

(defn- describe-segment
  [{:keys [mode code-points]} utf-8?]
  (let [payload (text/code-points->string code-points)]
    (cond-> {:mode mode :payload payload}
      (= :byte mode) (assoc :octets (segment-octets code-points utf-8?)))))

(defn- segment-count-fits?
  "True when the segment's declared count fits the class's count width.

  A count that overflows the width also always exceeds the class's data
  capacity, so skipping the class on overflow loses no valid plan."
  [{:keys [mode code-points]} version utf-8?]
  (let [width (case mode
                :numeric (segment/character-count-bit-width version)
                :alphanumeric (segment/alphanumeric-character-count-bit-width
                               version)
                :byte (segment/byte-character-count-bit-width version))
        declared-count (if (= :byte mode)
                         (count (segment-octets code-points utf-8?))
                         (count code-points))]
    (< declared-count (bit-shift-left 1 width))))

(def ^:private version-classes
  "Version ranges sharing character-count widths (Clause 7.4.1, Table 3)."
  [[1 9] [10 26] [27 40]])

(defn plan-text
  "Plans free text into optimal segments and the smallest fitting version.

      {:version 3
       :segments [{:mode :byte :payload \"tel:\" :octets [...]}
                  {:mode :numeric :payload \"420123456789\"}]
       :eci-designator nil          ; 26 when the text needs UTF-8
       :bit-vector [...]}           ; the complete unpadded data bit stream

  The bit vector includes the ECI header when present and every segment's
  mode, count, and payload bits; padding to the version's capacity is the
  caller's step."
  [payload-text error-correction-level]
  (when-not (and (string? payload-text) (seq payload-text))
    (fail! :invalid-text-payload
           "Text payload must be a non-empty string"
           {:payload payload-text
            :reason (if (string? payload-text)
                      :empty-payload
                      :non-string-payload)}))
  (let [code-points (text/string->code-points payload-text)
        utf-8? (boolean (some #(> % 255) code-points))
        planned
        (some
         (fn [[lowest-version highest-version]]
           (let [trace (plan-states code-points lowest-version utf-8?)
                 segments (backtrack-segments trace code-points)]
             (when (every? #(segment-count-fits? % lowest-version utf-8?)
                           segments)
               (let [bit-vector
                     (into (if utf-8?
                             (eci-header-bits utf-8-eci-designator)
                             [])
                           (mapcat #(segment-bit-vector
                                     % lowest-version utf-8?))
                           segments)
                     version
                     (some (fn [version]
                             (when (<= (count bit-vector)
                                       (* 8 (data-codeword-count
                                             version
                                             error-correction-level)))
                               version))
                           (range lowest-version (inc highest-version)))]
                 (when version
                   {:version version
                    :eci-designator (when utf-8? utf-8-eci-designator)
                    :bit-vector bit-vector
                    :segments (mapv #(describe-segment % utf-8?)
                                    segments)})))))
         version-classes)]
    (or planned
        (fail! :payload-too-large
               "Text exceeds ordinary QR Version 40 capacity at this level"
               {:character-count (count code-points)
                :error-correction-level error-correction-level}))))

(s/def ::planned-version ::parameters/version)

(s/fdef plan-text
  :args (s/cat :payload-text (s/and string? seq)
               :error-correction-level
               ::parameters/error-correction-level)
  :ret map?)
