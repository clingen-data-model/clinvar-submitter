(ns clinvar-submitter.report
  (:require [clojure.string :as str]
            [clojure-csv.core :as csv]
            [clojure.java.io :as io]))

(def exception-code-map
    {"*E-202" "Variant identifier not provided.",
     "*E-203" "Could not find GRCh38, GRCh37 or preferred representation.",
     "*E-301" "Interpretations with Path or Lik Path outcomes require a condition disease code or name and none was provided.",
     "*E-401" "Interpretation id not provided.",
     "*E-402" "Interpretation significance not provided.",
     "*E-403" "Interpretation evaluation date not provided.",
     "*E-501" "<x> met criteria rules and/or strength codes are invalid or missing."})

(defn get-exception
  [ecode]
  (let [code (first (str/split ecode #":"))
        desc (get exception-code-map code)
        type-str (subs (first (str/split code #"-")) 1)]
    {:type (cond (= "E" type-str) "Error" (= "W" type-str) "Warning ":else "Unknown")
     :code code
     :desc desc}))

(defn is-exception? [ecode]
  (let [exception-code (first (str/split ecode  #":"))]
    (not (nil? (get exception-code-map exception-code)))))

(defn get-record-exceptions [items]
  (let [exception-list
        (for [i (range (count items))]
          (if-not (nil? (get items i))
            (if (is-exception? (get items i))
              [(get items i) i])))]
    (filter some? exception-list)))

(defn get-exception-data-for-error [record error options]
    (let [ecode (first error)
          column (+ 1 (last error))
          record-number (last (str/split ecode  #":"))
          exception (get-exception ecode)
          exception-row [(str record-number)
                         (get options :input "--")
                         (get record 0)
                         (get record 25)
                         (str "[" record-number ", " column "]")
                         (get exception :type)
                         (get exception :code)
                         (get exception :desc)]]
      exception-row))

(defn get-exception-data-for-row [row options]
  (let [row-exception-list (get-record-exceptions row)
        results (reduce (fn [v err] (conj v (get-exception-data-for-error row err options))) [] row-exception-list)]
    results))

(defn get-webservice-exception-data-for-row [row options]
  (let [error-list (get-exception-data-for-row row options)
        ws-results (map (fn [error-array] (if (empty? error-array) [] {:errorCode (get error-array 6)
                                                                       :errorMessage (get error-array 7)})) error-list)]
    ws-results))
