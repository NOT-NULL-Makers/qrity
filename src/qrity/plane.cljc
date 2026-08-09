(ns ^:no-doc qrity.plane
  "Packed octet planes: the storage behind pixel-scale image values.

  A plane holds one octet (0-255) per element in a platform-packed array —
  a JVM byte array, a JavaScript Uint8Array — because pixel planes are the
  only structures in this codebase large enough for boxed vectors to hurt:
  measured on the JVM, a persistent-vector luminance plane costs ~17 bytes
  per pixel and a byte array ~1.5, with indexed access ~3.5x slower (see
  the luminance-representation gate in docs/decoding-exploration.md).

  The seam is deliberately narrow. Callers treat a plane as an immutable
  value: it is filled during construction — `from-values`, or `blank` plus
  `put!` inside the constructing function — and never mutated after being
  placed in an image value. JVM byte signedness never escapes: `value-at`
  masks to 0-255, so call sites read octets, not bytes. Planes lack value
  equality; compare or inspect through `values`.")

(defn blank
  "An all-zero plane of `octet-count` octets, to be filled during construction."
  [octet-count]
  #?(:clj (byte-array octet-count)
     :cljs (js/Uint8Array. octet-count)))

(defn length
  [plane]
  #?(:clj (alength ^bytes plane)
     :cljs (.-length plane)))

(defn value-at
  "The octet (0-255) at `index`.

  The JVM index cast is unchecked: a checked long→int conversion inside
  this accessor measured 5-8× on read-heavy paths once planes carried
  them, and `aget` itself still bounds-checks every access."
  [plane index]
  #?(:clj (bit-and (aget ^bytes plane (unchecked-int index)) 255)
     :cljs (aget plane index)))

(defn put!
  "Writes one octet during plane construction.

  Construction-time only: a plane already carried by an image value is
  never mutated."
  [plane index value]
  #?(:clj (aset ^bytes plane (unchecked-int index)
                (unchecked-byte value))
     :cljs (aset plane index value))
  plane)

(defn plane?
  [value]
  #?(:clj (bytes? value)
     :cljs (instance? js/Uint8Array value)))

(defn from-values
  "Builds a plane from a finite sequence of octets, validating the range.

  Validation is per element; bulk constructors on hot paths (the platform
  adapters, binarization) build via `blank` and `put!` instead, whose
  writes truncate rather than check — profiled: this constructor over a
  megapixel sequence is visible, `blank`/`put!` construction is not."
  [octet-values]
  (let [octets (vec octet-values)
        plane (blank (count octets))]
    (dotimes [index (count octets)]
      (let [value (nth octets index)]
        (when-not (and (int? value) (<= 0 value 255))
          (throw (ex-info "Plane values must be integers from 0 through 255"
                          {:qrity/error :invalid-plane-value
                           :index index
                           :value value})))
        (put! plane index value)))
    plane))

(defn values
  "The plane's contents as a vector, for comparison and inspection."
  [plane]
  (mapv #(value-at plane %) (range (length plane))))
