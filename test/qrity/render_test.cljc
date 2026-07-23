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
