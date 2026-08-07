(ns qrity.inspect-test
  (:require [clojure.string :as string]
            [qrity.encode :as encode]
            [qrity.inspect :as inspect]
            [qrity.plan :as plan]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(def mixed-payload "tel:+420123456789012345678901234567890")

(deftest the-census-accounts-for-every-module
  (doseq [{:keys [matrix]} [(encode/encode-numeric "8675309" :m)
                            (encode/encode-text
                             (apply str (take 220 (cycle "0123456789")))
                             :h)]]
    (let [{:keys [module-census dimension]}
          (inspect/symbol-properties matrix)]
      (is (= (* dimension dimension)
             (reduce + (vals module-census))))
      (is (not (contains? module-census :unclassified))))))

(deftest explains-known-function-modules
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)
        dimension (count matrix)
        explain #(inspect/explain-module matrix %)]
    (is (= {:role :finder-pattern :corner :top-left :element :outer-ring}
           (select-keys (explain [0 0]) [:role :corner :element])))
    (is (= {:role :timing-pattern :axis :horizontal}
           (select-keys (explain [6 8]) [:role :axis])))
    (is (= {:role :format-information
            :copy :primary
            :bit-index 0
            :field :error-correction-level}
           (select-keys (explain [8 0])
                        [:role :copy :bit-index :field])))
    (is (= :dark-module (:role (explain [(- dimension 8) 8]))))))

(deftest explains-alignment-and-version-information-on-larger-symbols
  (let [{:keys [matrix]} (encode/encode-text
                          (apply str (take 220 (cycle "0123456789")))
                          :h)
        dimension (count matrix)]
    (is (= {:role :alignment-pattern :element :center}
           (select-keys
            (inspect/explain-module matrix
                                    [(- dimension 7) (- dimension 7)])
            [:role :element])))
    (is (= {:role :version-information :copy :top-right}
           (select-keys
            (inspect/explain-module matrix [0 (- dimension 11)])
            [:role :copy])))))

(deftest module-explanations-reassemble-the-exact-bit-stream
  ;; Collect every data-codeword bit as the inspector explains it and
  ;; rebuild the message bit stream; its prefix must equal the planner's
  ;; bit vector for the same payload, bit for bit.
  (let [{:keys [matrix]} (encode/encode-text mixed-payload :m)
        {:keys [bit-vector]} (plan/plan-text mixed-payload :m)
        dimension (count matrix)
        stream-bits
        (into (sorted-map)
              (for [row (range dimension)
                    column (range dimension)
                    :let [explanation
                          (inspect/explain-module matrix [row column])
                          stream (:stream explanation)]
                    :when stream]
                [(:stream-bit-offset stream) (:data-bit explanation)]))]
    (is (= bit-vector
           (mapv stream-bits (range (count bit-vector)))))
    (is (= (set (range (count stream-bits)))
           (set (keys stream-bits)))
        "data-stream offsets must be contiguous from zero")))

(deftest every-segment-is-reachable-from-some-module
  (let [{:keys [matrix]} (encode/encode-text mixed-payload :m)
        dimension (count matrix)
        segment-indexes
        (into #{}
              (for [row (range dimension)
                    column (range dimension)
                    :let [stream (:stream (inspect/explain-module
                                           matrix [row column]))]
                    :when (:segment-index stream)]
                [(:segment-index stream) (:field stream)]))]
    (doseq [segment-index [0 1]
            field [:mode-indicator :character-count :payload]]
      (is (contains? segment-indexes [segment-index field])))))

(deftest renders-readable-reports
  (let [{:keys [matrix]} (encode/encode-text "žluťoučký kůň 42" :q)
        report (inspect/describe-symbol matrix)]
    (doseq [expected ["Ordinary QR symbol, Version"
                      "Error-correction level Q"
                      "ECI designator: 000026"
                      "žluťoučký kůň 42"
                      "Module census:"]]
      (testing expected
        (is (string/includes? report expected)))))
  (let [{:keys [matrix]} (encode/encode-numeric "8675309" :m)]
    (is (string/includes?
         (inspect/describe-module matrix [0 0])
         "finder pattern"))
    (is (string/includes?
         (inspect/describe-module matrix [12 12])
         "codeword"))))
