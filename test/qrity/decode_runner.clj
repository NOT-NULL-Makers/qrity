(ns qrity.decode-runner
  "Batch CLI: decode image files and print one JSON result per line.

  Used by scripts/mangle_and_verify.py to exercise the decoder against
  ImageMagick-mangled and foreign-encoder symbols in a single JVM run:

      clojure -M:decode-image out/a.png out/b.png ...

  The JSON is hand-assembled: string escaping via `pr-str` matches JSON
  for the characters that can occur here, and no dependency is worth one
  reporting format."
  (:require [clojure.string :as string]
            [qrity.image-io :as image-io]
            [qrity.scan :as scan]))

(defn- json-line
  [entries]
  (str "{"
       (string/join ","
                    (for [[key value] entries
                          :when (some? value)]
                      (str (pr-str key) ":"
                           (cond
                             (string? value) (pr-str value)
                             (vector? value)
                             (str "[" (string/join "," value) "]")
                             :else value))))
       "}"))

(defn- decode-file
  [path]
  (try
    (let [{:keys [payload octets version corrected-error-count
                  corrected-erasure-count mirrored? inverted?
                  eci-designator]}
          (scan/decode-luminance-image
           (image-io/read-luminance-image path))]
      (json-line
       [["path" path]
        ["status" "ok"]
        ["payload" payload]
        ["octets" octets]
        ["version" version]
        ["errors" corrected-error-count]
        ["erasures" corrected-erasure-count]
        ["mirrored" mirrored?]
        ["inverted" inverted?]
        ["eci" eci-designator]]))
    (catch Exception error
      (json-line
       [["path" path]
        ["status" (str (or (:qrity/error (ex-data error))
                           :unexpected-error))]
        ["message" (ex-message error)]]))))

(defn -main
  [& paths]
  (doseq [path paths]
    (println (decode-file path)))
  (shutdown-agents))
