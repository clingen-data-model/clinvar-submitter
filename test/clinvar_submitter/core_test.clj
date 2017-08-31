(ns clinvar-submitter.core-test
  (:require [clojure.test :refer :all]
            [clinvar-submitter.core :refer :all]
            [clinvar-submitter.ld :as ld]
            [clinvar-submitter.form :as form]))

(def interp-path "data/dmwg2.json")
(def context-path "data/context-new.jsonld")
(def t (ld/generate-symbol-table interp-path context-path))
(def m (vals t))
(def interps ((ld/prop= t "VariantInterpretation" "type") m))
(def i (first interps))
(def variant (form/get-variant t i))
(def expectedvalue (construct-variant t i))

(deftest test-variant-table
  (testing "Test correctness of variant table construction"
    (is (= (map #(construct-variant t %) interps) (construct-variant-table interp-path context-path))
    (println "Expectedvalue" (map #(construct-variant t %) interps)))))

; TODO Nafisa - can you initialize the 'expectedvalue' above using a 
; static string or object which is either hardcoded or comes from a
; test file that represents the final expected output.  As this stands
; it will always be true since both the expectedvalue and the test below
; use the same info to generate the compared values.
(deftest test-construct-variant
  (testing "Testing correctness of variant construction"
    (is (= expectedvalue (construct-variant t i))
    (println "Expectedvalue" expectedvalue))))

(run-tests)