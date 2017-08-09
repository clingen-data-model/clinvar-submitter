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

(deftest test-construct-variant
  (testing "Testing correctness ovariant construction"
    (is (= expectedvalue (construct-variant t i))
    (println "Expectedvalue" expectedvalue))))

(run-tests)