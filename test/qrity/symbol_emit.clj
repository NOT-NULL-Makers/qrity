(ns qrity.symbol-emit
  "CLI: encode free text and write a Plain PBM raster.

  Used by scripts/mangle_and_verify.py to produce this encoder's symbols
  for foreign decoders:

      clojure -M:emit-symbol PAYLOAD LEVEL OUTPUT.pbm [--inverted]"
  (:require [qrity.encode :as encode]
            [qrity.render :as render]))

(defn -main
  [& [payload level output-path & flags]]
  (when-not (and payload level output-path)
    (throw (ex-info "Expected PAYLOAD LEVEL OUTPUT.pbm [--inverted]"
                    {:qrity/error :invalid-emit-arguments})))
  (let [{:keys [matrix]} (encode/encode-text payload (keyword level))]
    (spit output-path
          (render/render-pbm matrix 8 4
                             {:inverted? (boolean
                                          (some #{"--inverted"} flags))})
          :encoding "UTF-8"))
  (shutdown-agents))
