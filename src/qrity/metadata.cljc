(ns qrity.metadata
  "Pure ordinary-QR format and version information calculations."
  (:require [clojure.spec.alpha :as s]
            [qrity.bits :as bits]
            [qrity.parameters :as parameters]))

(def format-information-bit-count 15)
(def version-information-bit-count 18)

(def ^:private format-generator 0x537)
(def ^:private format-xor-mask 0x5412)
(def ^:private version-generator 0x1F25)

(def ^:private error-correction-indicators
  {:l 1
   :m 0
   :q 3
   :h 2})

(s/def ::mask-reference ::parameters/mask-reference)
(s/def ::format-information-bits
  (s/coll-of #{0 1}
             :kind vector?
             :count format-information-bit-count))
(s/def ::version-information-bits
  (s/coll-of #{0 1}
             :kind vector?
             :count version-information-bit-count))

(defn- polynomial-remainder
  [shifted-value generator generator-degree highest-degree]
  (reduce (fn [value bit-index]
            (if (bit-test value bit-index)
              (bit-xor
               value
               (bit-shift-left generator
                               (- bit-index generator-degree)))
              value))
          shifted-value
          (range highest-degree (dec generator-degree) -1)))

(defn- require-error-correction-level!
  [error-correction-level]
  (when-not (contains? error-correction-indicators
                       error-correction-level)
    (throw
     (ex-info
      "Unknown ordinary QR error-correction level"
      {:qrity/error :invalid-error-correction-level
       :error-correction-level error-correction-level
       :supported-error-correction-levels
       parameters/error-correction-levels
       :clause "7.9.1"}))))

(defn- require-mask-reference!
  [mask-reference]
  (when-not (and (int? mask-reference)
                 (<= 0 mask-reference 7))
    (throw
     (ex-info
      "Ordinary QR mask reference must be an integer from 0 through 7"
      {:qrity/error :invalid-mask-reference
       :mask-reference mask-reference
       :clause "7.9.1"}))))

(defn- format-information-value
  [error-correction-level mask-reference]
  (require-error-correction-level! error-correction-level)
  (require-mask-reference! mask-reference)
  (let [data
        (bit-or
         (bit-shift-left
          (get error-correction-indicators error-correction-level)
          3)
         mask-reference)
        shifted (bit-shift-left data 10)
        remainder
        (polynomial-remainder shifted format-generator 10 14)]
    (bit-xor (bit-or shifted remainder) format-xor-mask)))

(defn format-information-bits
  "Returns the masked 15-bit ordinary-QR format word, most significant bit first.

  The one-argument form preserves the original fixed-profile level M behavior."
  ([mask-reference]
   (format-information-bits :m mask-reference))
  ([error-correction-level mask-reference]
   (bits/unsigned-integer->bits
    (format-information-value error-correction-level mask-reference)
    format-information-bit-count)))

(defn- require-version-information-version!
  [version]
  (when-not (and (int? version) (<= 1 version 40))
    (throw
     (ex-info
      "Ordinary QR version must be an integer from 1 through 40"
      {:qrity/error :invalid-version
       :version version
       :clause "7.10"})))
  (when (< version 7)
    (throw
     (ex-info
      "Ordinary QR Versions 1 through 6 do not contain version information"
      {:qrity/error :version-information-not-required
       :version version
       :clause "7.10"}))))

(defn version-information-bits
  "Returns the 18-bit version information for Version 7 through 40, MSB first."
  [version]
  (require-version-information-version! version)
  (let [shifted (bit-shift-left version 12)
        remainder
        (polynomial-remainder shifted version-generator 12 17)]
    (bits/unsigned-integer->bits
     (bit-or shifted remainder)
     version-information-bit-count)))

(s/fdef format-information-bits
  :args
  (s/alt :fixed-level
         (s/cat :mask-reference ::mask-reference)
         :explicit-level
         (s/cat :error-correction-level
                ::parameters/error-correction-level
                :mask-reference ::mask-reference))
  :ret ::format-information-bits)

(s/fdef version-information-bits
  :args (s/cat :version (s/int-in 7 41))
  :ret ::version-information-bits)
