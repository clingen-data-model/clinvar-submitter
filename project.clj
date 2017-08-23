(defproject clinvar-submitter "0.1.0-SNAPSHOT"
  :description "ClinGen ClinVar variant submission generation tool"
  :url "https://github.com/clingen-data-model/clinvar-submitter"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.github.jsonld-java/jsonld-java "0.10.0"]
                 [cheshire "5.7.1"]
                 [org.clojure/data.json "0.2.1"]
                 [clojure-csv/clojure-csv "2.0.1"]]
  :main ^:skip-aot clinvar-submitter.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
