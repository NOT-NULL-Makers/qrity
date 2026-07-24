(ns qrity.render-test
  (:require [clojure.string :as string]
            [qrity.encode :as encode]
            [qrity.render :as render]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(defn parse-plain-pbm
  [pbm]
  (let [[magic dimensions & raster-lines] (string/split-lines pbm)
        [width height] (mapv #(parse-long %)
                             (string/split dimensions #" "))
        pixels (mapv #(- (int %) (int \0))
                     (apply str raster-lines))]
    {:magic magic
     :width width
     :height height
     :pixels (mapv vec (partition width pixels))}))

(deftest renders-binary-modules-with-square-terminal-cells
  (is (= (string/join
          "\n"
          ["        "
           "  \u2588\u2588    "
           "    \u2588\u2588  "
           "        "])
         (render/render-unicode [[1 0]
                                 [0 1]]
                                1))))

(deftest renders-a-complete-symbol-with-the-default-quiet-zone
  (let [matrix (get-in (encode/encode-numeric-v1-m "8675309")
                       [:symbol :matrix])
        output (render/render-unicode matrix)
        lines (string/split output #"\n")
        dark-module-count (count (filter #(= \u2588 %) output))]
    (is (= 29 (count lines)))
    (is (every? #(= 58 (count %)) lines))
    (is (every? #(every? #{\space} %)
                (concat (take 4 lines) (take-last 4 lines))))
    (is (= (* 2 (count (filter #{1} (mapcat identity matrix))))
           dark-module-count))
    (is (not (string/ends-with? output "\n")))))

(deftest zero-width-quiet-zone-is-supported-explicitly
  (is (= "\u2588\u2588"
         (render/render-unicode [[1]] 0))))

(deftest rejects-malformed-renderer-input
  (doseq [[value reason]
          [[[] :invalid-matrix]
           [[[1 0]] :invalid-matrix]
           [[[1 2] [0 1]] :invalid-matrix]]]
    (testing (pr-str value)
      (is (= {:qrity/error :invalid-render-input
              :renderer :unicode
              :reason reason
              :value value}
             (exception-data #(render/render-unicode value))))))
  (is (= :invalid-quiet-zone
         (:reason
          (exception-data #(render/render-unicode [[1]] -1))))))

(deftest plain-pbm-has-canonical-header-polarity-and-newline
  (is (= "P1\n4 4\n0000010000100000\n"
         (render/render-pbm [[1 0]
                             [0 1]]
                            1
                            1))))

(deftest plain-pbm-preserves-square-modules-scale-and-quiet-zone
  (let [matrix [[1 0]
                [0 1]]
        pbm (render/render-pbm matrix 3 2)
        {:keys [magic width height pixels]} (parse-plain-pbm pbm)]
    (is (= "P1" magic))
    (is (= 18 width height))
    (is (= (* width height) (count (mapcat identity pixels))))
    (doseq [row (range height)
            column (range width)]
      (let [inside-row (- row 6)
            inside-column (- column 6)
            expected
            (if (and (<= 0 inside-row 5)
                     (<= 0 inside-column 5))
              (get-in matrix [(quot inside-row 3)
                              (quot inside-column 3)])
              0)]
        (is (= expected (get-in pixels [row column]))
            (pr-str [row column]))))))

(deftest generated-plain-pbm-is-deterministic-and-portable
  (let [matrix (get-in (encode/encode-numeric-v1-m "8675309")
                       [:symbol :matrix])
        first-output (render/render-pbm matrix)
        second-output (render/render-pbm matrix)
        lines (string/split-lines first-output)
        parsed (parse-plain-pbm first-output)]
    (is (= first-output second-output))
    (is (string/ends-with? first-output "\n"))
    (is (every? #(<= (count %) render/plain-pbm-line-length) lines))
    (is (= 232 (:width parsed) (:height parsed)))
    (is (= (* 232 232)
           (count (mapcat identity (:pixels parsed)))))))

(deftest plain-pbm-rejects-invalid-options
  (is (= {:qrity/error :invalid-render-input
          :renderer :pbm
          :reason :invalid-matrix
          :value []}
         (exception-data #(render/render-pbm []))))
  (doseq [pixel-scale [0 -1 1.5]]
    (let [data (exception-data
                #(render/render-pbm [[1]] pixel-scale 4))]
      (is (= :pbm (:renderer data)))
      (is (= :invalid-pixel-scale (:reason data)))))
  (doseq [quiet-zone [-1 1.5]]
    (let [data (exception-data
                #(render/render-pbm [[1]] 8 quiet-zone))]
      (is (= :pbm (:renderer data)))
      (is (= :invalid-quiet-zone (:reason data))))))
