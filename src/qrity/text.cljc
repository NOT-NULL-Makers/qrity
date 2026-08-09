(ns ^:no-doc qrity.text
  "Pure UTF-8 transcoding between strings and octet vectors.

  Implemented from the code-point definition rather than platform codecs so
  Clojure and ClojureScript produce identical octets and identical failures:
  strings are read as UTF-16 code units with surrogate pairs combined and
  lone surrogates rejected; octets are validated strictly — continuation
  shape, overlong forms, surrogate code points, and the U+10FFFF ceiling all
  fail with structured errors. Used for ECI 000026 (UTF-8) payloads."
  (:require [clojure.spec.alpha :as s]))

(defn- fail!
  [error message data]
  (throw
   (ex-info message (assoc data :qrity/error error :clause "7.4.2"))))

(defn- code-unit
  [text index]
  #?(:clj (int (.charAt ^String text index))
     :cljs (.charCodeAt text index)))

(defn- high-surrogate? [unit] (<= 0xD800 unit 0xDBFF))
(defn- low-surrogate? [unit] (<= 0xDC00 unit 0xDFFF))

(defn string->code-points
  "Reads a string as Unicode code points, rejecting lone surrogates."
  [text]
  (when-not (string? text)
    (fail! :invalid-text
           "Text must be a string"
           {:text text :reason :non-string-text}))
  (loop [index 0
         code-points []]
    (if (= index (count text))
      code-points
      (let [unit (code-unit text index)]
        (cond
          (high-surrogate? unit)
          (let [next-unit (when (< (inc index) (count text))
                            (code-unit text (inc index)))]
            (when-not (and next-unit (low-surrogate? next-unit))
              (fail! :invalid-text
                     "Text contains a lone high surrogate"
                     {:reason :lone-surrogate :code-unit-index index}))
            (recur (+ index 2)
                   (conj code-points
                         (+ 0x10000
                            (bit-shift-left (- unit 0xD800) 10)
                            (- next-unit 0xDC00)))))

          (low-surrogate? unit)
          (fail! :invalid-text
                 "Text contains a lone low surrogate"
                 {:reason :lone-surrogate :code-unit-index index})

          :else
          (recur (inc index) (conj code-points unit)))))))

(defn code-points->string
  [code-points]
  (apply str
         (mapcat (fn [code-point]
                   (if (< code-point 0x10000)
                     [(char code-point)]
                     (let [offset (- code-point 0x10000)]
                       [(char (+ 0xD800 (bit-shift-right offset 10)))
                        (char (+ 0xDC00 (bit-and offset 0x3FF)))])))
                 code-points)))

(defn code-point-octet-count
  "How many UTF-8 octets one code point occupies."
  [code-point]
  (cond
    (< code-point 0x80) 1
    (< code-point 0x800) 2
    (< code-point 0x10000) 3
    :else 4))

(defn- code-point->octets
  [code-point]
  (case (code-point-octet-count code-point)
    1 [code-point]
    2 [(bit-or 0xC0 (bit-shift-right code-point 6))
       (bit-or 0x80 (bit-and code-point 0x3F))]
    3 [(bit-or 0xE0 (bit-shift-right code-point 12))
       (bit-or 0x80 (bit-and (bit-shift-right code-point 6) 0x3F))
       (bit-or 0x80 (bit-and code-point 0x3F))]
    4 [(bit-or 0xF0 (bit-shift-right code-point 18))
       (bit-or 0x80 (bit-and (bit-shift-right code-point 12) 0x3F))
       (bit-or 0x80 (bit-and (bit-shift-right code-point 6) 0x3F))
       (bit-or 0x80 (bit-and code-point 0x3F))]))

(defn utf-8-octets
  "Encodes a string into a vector of UTF-8 octets."
  [text]
  (into [] (mapcat code-point->octets) (string->code-points text)))

(defn- require-continuation!
  [octets index]
  (let [octet (nth octets index nil)]
    (when-not (and octet (= 0x80 (bit-and octet 0xC0)))
      (fail! :invalid-utf-8
             "A UTF-8 sequence is truncated or has a malformed continuation"
             {:reason :malformed-continuation :octet-index index}))
    (bit-and octet 0x3F)))

(defn utf-8-text
  "Strictly decodes a vector of UTF-8 octets into a string."
  [octets]
  (loop [index 0
         code-points []]
    (if (= index (count octets))
      (code-points->string code-points)
      (let [octet (nth octets index)
            [length code-point]
            (cond
              (< octet 0x80) [1 octet]
              (= 0xC0 (bit-and octet 0xE0))
              [2 (bit-or (bit-shift-left (bit-and octet 0x1F) 6)
                         (require-continuation! octets (inc index)))]
              (= 0xE0 (bit-and octet 0xF0))
              [3 (bit-or (bit-shift-left (bit-and octet 0x0F) 12)
                         (bit-shift-left
                          (require-continuation! octets (inc index)) 6)
                         (require-continuation! octets (+ index 2)))]
              (= 0xF0 (bit-and octet 0xF8))
              [4 (bit-or (bit-shift-left (bit-and octet 0x07) 18)
                         (bit-shift-left
                          (require-continuation! octets (inc index)) 12)
                         (bit-shift-left
                          (require-continuation! octets (+ index 2)) 6)
                         (require-continuation! octets (+ index 3)))]
              :else
              (fail! :invalid-utf-8
                     "An octet is not a valid UTF-8 sequence lead"
                     {:reason :invalid-lead-octet
                      :octet-index index
                      :octet octet}))]
        (when (not= length (code-point-octet-count code-point))
          (fail! :invalid-utf-8
                 "A UTF-8 sequence uses an overlong form"
                 {:reason :overlong-form :octet-index index}))
        (when (<= 0xD800 code-point 0xDFFF)
          (fail! :invalid-utf-8
                 "A UTF-8 sequence encodes a surrogate code point"
                 {:reason :surrogate-code-point :octet-index index}))
        (when (> code-point 0x10FFFF)
          (fail! :invalid-utf-8
                 "A UTF-8 sequence exceeds U+10FFFF"
                 {:reason :beyond-unicode :octet-index index}))
        (recur (+ index length) (conj code-points code-point))))))

(s/def ::code-points
  (s/coll-of #(and (int? %) (<= 0 % 0x10FFFF)) :kind vector?))

(s/fdef utf-8-octets
  :args (s/cat :text string?)
  :ret (s/coll-of #(and (int? %) (<= 0 % 255)) :kind vector?))
