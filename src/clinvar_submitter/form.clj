(ns clinvar-submitter.form
 (:require [clinvar-submitter.ld :as ld :refer [ld-> ld1-> prop=]]))
  
 (defn csv-colval
	 "Outputs a well-formed value for output of the CSV file being generated. 
	   nil should be empty strings, non-strings should be stringified, etc..."
	 [v]
	 (if (nil? v) "" (if (number? v) (str v) v)))
 