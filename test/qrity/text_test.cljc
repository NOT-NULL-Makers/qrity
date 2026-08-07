(ns qrity.text-test
  (:require [qrity.text :as text]
            #?(:clj [clojure.test :refer [deftest is testing]]
               :cljs [cljs.test :refer-macros [deftest is testing]])))

(defn exception-data
  [thunk]
  (try
    (thunk)
    nil
    (catch #?(:clj clojure.lang.ExceptionInfo :cljs :default) error
      (ex-data error))))

(deftest encodes-known-utf-8-sequences
  (is (= [0x24] (text/utf-8-octets "$")))
  (is (= [0xC2 0xA3] (text/utf-8-octets "£")))
  (is (= [0xC5 0x99] (text/utf-8-octets "ř")))
  (is (= [0xE2 0x82 0xAC] (text/utf-8-octets "€")))
  (is (= [0xF0 0x9F 0x98 0x80] (text/utf-8-octets "😀"))))

(deftest round-trips-mixed-plane-text
  (doseq [payload ["Příliš žluťoučký kůň úpěl ďábelské ódy"
                   "日本語テキスト"
                   "mixed ASCII + čeština + 😀🎉 + עברית"]]
    (testing payload
      (is (= payload (text/utf-8-text (text/utf-8-octets payload)))))))

(deftest rejects-lone-surrogates
  (is (= :invalid-text
         (:qrity/error
          (exception-data
           #(text/utf-8-octets (str "a" (char 0xD83D))))))))

(deftest rejects-malformed-utf-8-octets
  (doseq [[octets reason] [[[0xC2] :malformed-continuation]
                           [[0xE2 0x82] :malformed-continuation]
                           [[0x80] :invalid-lead-octet]
                           [[0xFF] :invalid-lead-octet]
                           [[0xC0 0xAF] :overlong-form]
                           [[0xE0 0x80 0xAF] :overlong-form]
                           [[0xED 0xA0 0x80] :surrogate-code-point]
                           [[0xF4 0x90 0x80 0x80] :beyond-unicode]]]
    (testing (pr-str octets)
      (let [failure (exception-data #(text/utf-8-text octets))]
        (is (= :invalid-utf-8 (:qrity/error failure)))
        (is (= reason (:reason failure)))))))
