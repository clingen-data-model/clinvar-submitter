(ns clinvar-submitter.core-test
  (:require [clojure.test :refer :all]
            [clinvar-submitter.core :refer :all]))

(deftest test-variant-table
  (testing "Test correctness of variant table construction"
    (is (= '(["" "" "" "NC_000012.12" "" "" ""])
           (construct-variant-table "data/test_interp_1.dmwg.json" "data/context-new.jsonld")))))

;(deftest test-xyz
;  (testing "this is the description of what is being tested"
;		 (is (= '([])
;		        (functiontotest arg1 arg2 ...)))))

  
  
  
;; (deftest a-test
;;   (testing "FIXME, I fail."
;;     (is (= 0 1))))
