(ns qrity.mask
  "Pure ordinary-QR mask candidate construction, scoring, and selection."
  (:require [clojure.spec.alpha :as s]
            [qrity.matrix :as matrix]
            [qrity.message :as message]
            [qrity.parameters :as parameters]
            [qrity.validation :as validation]))

(def penalty-keys
  #{:same-color-runs
    :same-color-blocks
    :finder-like-patterns
    :dark-proportion})

(def candidate-keys
  #{:version
    :error-correction-level
    :mask-reference
    :matrix
    :penalties
    :total-penalty})

(defn ordinary-bit-matrix?
  "Checks a QR-dimension square matrix containing only 0 and 1.

  Shape alone cannot prove symbol provenance or whether a quiet zone was omitted."
  [value]
  (and
   (vector? value)
   (let [dimension (count value)]
     (and
      (<= 21 dimension 177)
      (zero? (mod (- dimension 17) 4))
      (every? #(and (vector? %)
                    (= dimension (count %))
                    (every? #{0 1} %))
              value)))))

;; Scoring runs eight times per symbol (once per mask candidate) over every
;; module twice (rows and columns), so the scorers below are indexed loops
;; over line accessors — profiled at ~60% of a whole encode when they ran
;; on sequences with a materialized transpose per candidate.

(defn- horizontal-line
  [bit-matrix line-index]
  (let [row (nth bit-matrix line-index)]
    (fn [position] (nth row position))))

(defn- vertical-line
  [bit-matrix line-index]
  (fn [position] (nth (nth bit-matrix position) line-index)))

(defn- line-run-penalty
  [cell-at length]
  (loop [position 1
         previous (cell-at 0)
         run-length 1
         penalty 0]
    (if (= position length)
      (+ penalty (if (< run-length 5) 0 (- run-length 2)))
      (let [cell (cell-at position)]
        (if (= cell previous)
          (recur (inc position) previous (inc run-length) penalty)
          (recur (inc position) cell 1
                 (+ penalty (if (< run-length 5)
                              0
                              (- run-length 2)))))))))

(defn- both-orientations-total
  "Sums a line score over every row and every column.

  Width and height are independent: the component scorers accept the
  rectangular fixtures their tests probe them with, exactly as the
  sequence-based originals did."
  [bit-matrix line-score]
  (let [height (count bit-matrix)
        width (count (nth bit-matrix 0))
        row-total (loop [row 0
                         total 0]
                    (if (= row height)
                      total
                      (recur (inc row)
                             (+ total
                                (line-score
                                 (horizontal-line bit-matrix row)
                                 width)))))]
    (loop [column 0
           total row-total]
      (if (= column width)
        total
        (recur (inc column)
               (+ total
                  (line-score (vertical-line bit-matrix column)
                              height)))))))

(defn same-color-runs-penalty
  "Returns Table 11 penalty N1 for maximal horizontal and vertical runs."
  [bit-matrix]
  (both-orientations-total bit-matrix line-run-penalty))

(defn same-color-blocks-penalty
  "Returns Table 11 penalty N2 for overlapping monochrome 2-by-2 blocks."
  [bit-matrix]
  (let [dimension (count bit-matrix)]
    (* 3
       (loop [row 0
              blocks 0]
         (if (= row (dec dimension))
           blocks
           (let [this-row (nth bit-matrix row)
                 next-row (nth bit-matrix (inc row))
                 blocks
                 (loop [column 0
                        blocks blocks]
                   (if (= column (dec dimension))
                     blocks
                     (let [cell (nth this-row column)]
                       (recur (inc column)
                              (if (and (= cell (nth this-row
                                                    (inc column)))
                                       (= cell (nth next-row column))
                                       (= cell (nth next-row
                                                    (inc column))))
                                (inc blocks)
                                blocks)))))]
             (recur (inc row) blocks)))))))

(def finder-like-core
  [1 0 1 1 1 0 1])

(defn- light-context-before?
  [cell-at start]
  (loop [position (max 0 (- start 4))]
    (cond
      (= position start) true
      (zero? (cell-at position)) (recur (inc position))
      :else false)))

(defn- light-context-after?
  [cell-at end length]
  (let [context-end (min length (+ end 4))]
    (loop [position end]
      (cond
        (= position context-end) true
        (zero? (cell-at position)) (recur (inc position))
        :else false))))

(defn- finder-like-line-count
  [cell-at length]
  (loop [start 0
         found 0]
    (if (> start (- length 7))
      found
      (recur (inc start)
             (if (and (= 1 (cell-at start))
                      (= 0 (cell-at (+ start 1)))
                      (= 1 (cell-at (+ start 2)))
                      (= 1 (cell-at (+ start 3)))
                      (= 1 (cell-at (+ start 4)))
                      (= 0 (cell-at (+ start 5)))
                      (= 1 (cell-at (+ start 6)))
                      (or (light-context-before? cell-at start)
                          (light-context-after? cell-at (+ start 7)
                                                length)))
               (inc found)
               found)))))

(defn finder-like-patterns-penalty
  "Returns Table 11 penalty N3 for horizontal and vertical `1011101` cores.

  A core is counted once even when both sides are light. For this rule only, a
  light run reaching a symbol edge is continued by the required quiet zone."
  [bit-matrix]
  (* 40 (both-orientations-total bit-matrix finder-like-line-count)))

(defn dark-proportion-penalty
  "Returns Table 11 penalty N4 using exact integer arithmetic."
  [bit-matrix]
  (let [dimension (count bit-matrix)
        total (* dimension dimension)
        dark (loop [row 0
                    dark 0]
               (if (= row dimension)
                 dark
                 (let [this-row (nth bit-matrix row)]
                   (recur (inc row)
                          (loop [column 0
                                 dark dark]
                            (if (= column dimension)
                              dark
                              (recur (inc column)
                                     (+ dark
                                        (nth this-row column)))))))))]
    (* 10
       (quot (abs (- (* 20 dark) (* 10 total)))
             total))))

(defn penalty-components
  "Scores a supplied QR-dimension bit matrix under Table 11.

  Candidate construction guarantees complete-symbol provenance and omission of the
  quiet zone. A standalone caller must supply that context; bit-matrix shape cannot
  prove it. The map exposes four weighted components whose values sum to the total."
  [bit-matrix]
  (when-not (ordinary-bit-matrix? bit-matrix)
    (throw
     (ex-info
      "Mask scoring requires a complete ordinary QR bit matrix"
      {:qrity/error :invalid-scoring-matrix
       :reason :noncanonical-ordinary-bit-matrix
       :actual-dimension (when (vector? bit-matrix)
                           (count bit-matrix))
       :clause "7.8.3.1"})))
  {:same-color-runs
   (same-color-runs-penalty bit-matrix)
   :same-color-blocks
   (same-color-blocks-penalty bit-matrix)
   :finder-like-patterns
   (finder-like-patterns-penalty bit-matrix)
   :dark-proportion
   (dark-proportion-penalty bit-matrix)})

(defn candidate-structure?
  "Checks candidate shape and internally consistent scoring.

  Use `candidate-matches?` to establish provenance from a final message and its
  placement."
  [value]
  (and
   (map? value)
   (= candidate-keys (set (keys value)))
   (s/valid? ::parameters/version (:version value))
   (s/valid? ::parameters/error-correction-level
             (:error-correction-level value))
   (s/valid? ::parameters/mask-reference (:mask-reference value))
   (ordinary-bit-matrix? (:matrix value))
   (= (:dimension
       (parameters/ordinary-qr-parameters
        (:version value)
        (:error-correction-level value)))
      (count (:matrix value)))
   (map? (:penalties value))
   (= penalty-keys (set (keys (:penalties value))))
   (every? #(and (int? %) (not (neg? %)))
           (vals (:penalties value)))
   (= (:penalties value)
      (penalty-components (:matrix value)))
   (= (:total-penalty value)
      (reduce + (vals (:penalties value))))))

(defn- require-canonical-candidate-input!
  [final-message placement]
  (when-not (message/final-message? final-message)
    (throw
     (ex-info
      "Mask candidates require a canonical final message"
      {:qrity/error :invalid-final-message
       :reason :noncanonical-final-message
       :clause "7.6/7.8.3"})))
  (when-not (matrix/placement-structure? placement)
    (throw
     (ex-info
      "Mask candidates require a canonical unmasked placement"
      {:qrity/error :invalid-placement
       :reason :noncanonical-placement
       :clause "7.7.3/7.8.3"})))
  (let [expected-dimension
        (:dimension
         (parameters/ordinary-qr-parameters
          (:version final-message)
          (:error-correction-level final-message)))
        actual-dimension (count (:matrix placement))]
    (when-not (= expected-dimension actual-dimension)
      (throw
       (ex-info
        "Placement version does not match the supplied final message"
        {:qrity/error :message-placement-version-mismatch
         :version (:version final-message)
         :expected-dimension expected-dimension
         :actual-dimension actual-dimension
         :clause "7.7.3/7.8.3"}))))
  (when-not
   (matrix/placement-matches-message-bits?
    placement
    (:message-bits final-message))
    (throw
     (ex-info
      "Placement does not contain the supplied final message"
      {:qrity/error :placement-message-mismatch
       :version (:version final-message)
       :error-correction-level
       (:error-correction-level final-message)
       :clause "7.7.3/7.8.3"}))))

(defn- require-candidate-input!
  [final-message placement]
  (when validation/*canonical-checks?*
    (require-canonical-candidate-input! final-message placement)))

(defn- candidate-source-request?
  [{:keys [final-message placement]}]
  (try
    (and
     (message/final-message? final-message)
     (matrix/placement-structure? placement)
     (= (:dimension
         (parameters/ordinary-qr-parameters
          (:version final-message)
          (:error-correction-level final-message)))
        (count (:matrix placement)))
     (matrix/placement-matches-message-bits?
      placement
      (:message-bits final-message)))
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (validation/rejected error))))

(defn- candidate-request?
  [{:keys [mask-reference] :as request}]
  (and
   (candidate-source-request? request)
   (s/valid? ::parameters/mask-reference mask-reference)))

(defn- build-candidate
  [final-message placement mask-reference]
  (let [version (:version final-message)
        error-correction-level
        (:error-correction-level final-message)
        bit-matrix
        (matrix/candidate-bit-matrix (:matrix placement)
                                     error-correction-level
                                     mask-reference)
        penalties (penalty-components bit-matrix)]
    {:version version
     :error-correction-level error-correction-level
     :mask-reference mask-reference
     :matrix bit-matrix
     :penalties penalties
     :total-penalty (reduce + (vals penalties))}))

(defn mask-candidate
  "Builds and scores one provenance-bound complete-symbol candidate.

  Canonical input re-validation runs only under
  `qrity.validation/*canonical-checks?*`; the mask-reference check always runs."
  [final-message placement mask-reference]
  (require-candidate-input! final-message placement)
  (when-not (s/valid? ::parameters/mask-reference mask-reference)
    (throw
     (ex-info
      "Ordinary QR mask reference must be an integer from 0 through 7"
      {:qrity/error :invalid-mask-reference
       :mask-reference mask-reference
       :clause "7.8.2"})))
  (build-candidate final-message placement mask-reference))

(defn mask-candidates
  "Builds all eight complete candidates independently from one placement.

  Canonical input re-validation runs only under
  `qrity.validation/*canonical-checks?*`."
  [final-message placement]
  (require-candidate-input! final-message placement)
  (mapv #(build-candidate final-message placement %) (range 8)))

(defn candidate-matches?
  "Checks candidate provenance, metadata binding, matrix, and score."
  [final-message placement candidate]
  (and
   (message/final-message? final-message)
   (matrix/placement-matches-message-bits?
    placement
    (:message-bits final-message))
   (candidate-structure? candidate)
   (= candidate
      (build-candidate
       final-message
       placement
       (:mask-reference candidate)))))

(defn candidate-set-matches?
  "Checks the ordered eight-candidate set and its source provenance."
  [final-message placement candidates]
  (and
   (message/final-message? final-message)
   (matrix/placement-matches-message-bits?
    placement
    (:message-bits final-message))
   (vector? candidates)
   (= (vec (range 8)) (mapv :mask-reference candidates))
   (every?
    #(candidate-matches? final-message placement %)
    candidates)))

(defn- candidate-set?
  [candidates]
  (and
   (vector? candidates)
   (= (vec (range 8)) (mapv :mask-reference candidates))
   (every? candidate-structure? candidates)
   (apply = (map :version candidates))
   (apply = (map :error-correction-level candidates))))

(defn- require-candidate-set!
  [candidates]
  (when (and validation/*canonical-checks?*
             (not (candidate-set? candidates)))
    (throw
     (ex-info
      "Selection requires the ordered candidates for mask references 0 through 7"
      {:qrity/error :invalid-mask-candidates
       :reason :noncanonical-candidate-set
       :clause "7.8.3.1"}))))

(defn minimum-penalty-candidates
  "Returns every globally minimum-penalty candidate in mask-reference order.

  Canonical candidate-set re-validation runs only under
  `qrity.validation/*canonical-checks?*`."
  [candidates]
  (require-candidate-set! candidates)
  (let [minimum (apply min (map :total-penalty candidates))]
    (into []
          (filter #(= minimum (:total-penalty %)))
          candidates)))

(defn minimum-candidates-match?
  "Checks that `minima` contains exactly every global minimum, in input order."
  [candidates minima]
  (and
   (candidate-set? candidates)
   (vector? minima)
   (seq minima)
   (let [minimum (apply min (map :total-penalty candidates))]
     (= minima
        (into []
              (filter #(= minimum (:total-penalty %)))
              candidates)))))

(defn select-best-candidate
  "Builds all candidates and returns a global minimum.

  ISO/IEC 18004 requires a lowest-penalty mask but specifies no tie-break.
  QRity's deterministic reproducibility policy chooses the lowest numeric mask
  reference among tied minima."
  [final-message placement]
  (first
   (minimum-penalty-candidates
    (mask-candidates final-message placement))))

(s/def ::ordinary-bit-matrix ordinary-bit-matrix?)
(s/def ::same-color-runs nat-int?)
(s/def ::same-color-blocks nat-int?)
(s/def ::finder-like-patterns nat-int?)
(s/def ::dark-proportion nat-int?)
(s/def ::penalties
  (s/keys :req-un
          [::same-color-runs
           ::same-color-blocks
           ::finder-like-patterns
           ::dark-proportion]))
(s/def ::total-penalty nat-int?)
(s/def ::candidate-structure candidate-structure?)
(s/def ::candidates
  (s/and
   (s/coll-of ::candidate-structure :kind vector? :count 8)
   candidate-set?))
(s/def ::candidate-source-request
  (s/and
   (s/cat :final-message any? :placement any?)
   candidate-source-request?))
(s/def ::candidate-request
  (s/and
   (s/cat :final-message any?
          :placement any?
          :mask-reference any?)
   candidate-request?))

(s/fdef penalty-components
  :args (s/cat :bit-matrix ::ordinary-bit-matrix)
  :ret ::penalties)

(s/fdef mask-candidate
  :args ::candidate-request
  :ret ::candidate-structure
  :fn
  (fn [{:keys [args ret]}]
    (candidate-matches?
     (:final-message args)
     (:placement args)
     ret)))

(s/fdef mask-candidates
  :args ::candidate-source-request
  :ret ::candidates
  :fn
  (fn [{:keys [args ret]}]
    (candidate-set-matches?
     (:final-message args)
     (:placement args)
     ret)))

(s/fdef minimum-penalty-candidates
  :args (s/cat :candidates ::candidates)
  :ret (s/coll-of ::candidate-structure
                  :kind vector?
                  :min-count 1)
  :fn
  (fn [{:keys [args ret]}]
    (minimum-candidates-match?
     (:candidates args)
     ret)))

(s/fdef select-best-candidate
  :args ::candidate-source-request
  :ret ::candidate-structure
  :fn
  (fn [{:keys [args ret]}]
    (= ret
       (first
        (minimum-penalty-candidates
         (mask-candidates
          (:final-message args)
          (:placement args)))))))
