(ns qrity.plan-test
  (:require [qrity.decode :as decode]
            [qrity.encode :as encode]
            [qrity.plan :as plan]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(deftest single-class-payloads-plan-as-single-segments
  (testing "pure digits stay Numeric"
    (is (= [:numeric]
           (mapv :mode (:segments (plan/plan-text "8675309000123" :m))))))
  (testing "Table 5 text stays Alphanumeric"
    (is (= [:alphanumeric]
           (mapv :mode
                 (:segments (plan/plan-text "HELLO WORLD $1" :m))))))
  (testing "short mixed text collapses into one Byte segment"
    (is (= [:byte]
           (mapv :mode (:segments (plan/plan-text "a1B2c3" :m)))))))

(deftest long-runs-earn-their-own-segments
  (let [{:keys [segments]}
        (plan/plan-text "tel:+420123456789012345678901234567890" :m)]
    (is (= [:byte :numeric] (mapv :mode segments))
        "a long digit run after byte text switches to Numeric")))

(deftest planned-bits-never-exceed-single-mode-encodings
  (doseq [payload ["tel:+420123456789012345678901234567890"
                   "HTTPS://EXAMPLE.COM/A-1 0123456789012345"
                   "abcDEF012345678901234567890xyz"
                   "8675309"]]
    (testing payload
      (let [planned (count (:bit-vector (plan/plan-text payload :m)))
            byte-only (+ 4 8 (* 8 (count payload)))]
        (is (<= planned byte-only)
            "the plan must never lose to naive all-Byte encoding")))))

(deftest utf-8-text-switches-to-eci
  (let [{:keys [eci-designator segments]}
        (plan/plan-text "žluťoučký kůň" :m)]
    (is (= 26 eci-designator))
    (is (every? #(= :byte (:mode %)) segments))))

(deftest latin-1-text-stays-on-the-default-interpretation
  (is (nil? (:eci-designator (plan/plan-text "café façade" :m)))))

(deftest refuses-empty-and-oversized-text
  (is (= :invalid-text-payload
         (:qrity/error (exception-data #(plan/plan-text "" :m)))))
  (is (= :payload-too-large
         (:qrity/error
          (exception-data
           #(plan/plan-text
             (apply str (repeat 8000 "x")) :h))))))

(defn- round-trip
  [payload level]
  (decode/decode-matrix (:matrix (encode/encode-text payload level))))

(deftest encode-text-round-trips-through-the-decoder
  (doseq [payload ["8675309"
                   "HELLO WORLD"
                   "tel:+420123456789012345678901234567890"
                   "https://example.com/qr?id=123456789012345&lang=cs"
                   "Příliš žluťoučký kůň úpěl ďábelské ódy 12345678901234"
                   "emoji 😀🎉 and 0123456789 digits"
                   "café façade No. 0123456789012345"]]
    (testing payload
      (let [decoded (round-trip payload :m)]
        (is (= payload (:payload decoded)))
        (is (= (:payload decoded)
               (apply str (map :payload (:segments decoded)))))))))

(deftest decoded-segments-report-the-eci-designator
  (let [decoded (round-trip "žluťoučký kůň 42" :q)]
    (is (= 26 (:eci-designator decoded)))
    (is (= "žluťoučký kůň 42" (:payload decoded)))))

(deftest multi-segment-symbols-report-mixed-mode
  (let [decoded (round-trip
                 "tel:+420123456789012345678901234567890" :m)]
    (is (= :mixed (:mode decoded)))
    (is (= 2 (count (:segments decoded))))))

(deftest automatic-version-selection-is-minimal
  (let [payload (apply str (take 156 (cycle "0123456789")))
        symbol-value (encode/encode-text payload :m)]
    ;; 156 digits at level M need Version 5 (capacity 156) exactly.
    (is (= 5 (:version symbol-value)))
    (is (= payload (:payload (decode/decode-matrix
                              (:matrix symbol-value)))))))
