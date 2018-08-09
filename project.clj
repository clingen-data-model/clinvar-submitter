(defproject clinvar-submitter "1.0.0"
  :description "ClinGen ClinVar variant submission generation tool"
  :url "https://github.com/clingen-data-model/clinvar-submitter"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.github.jsonld-java/jsonld-java "0.10.0"]
                 [metosin/scjsv "0.4.0"]
                 [cheshire "5.7.1"]
                 [org.clojure/data.json "0.2.1"]
                 [clojure-csv/clojure-csv "2.0.1"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/tools.logging "0.2.4"]
                 [org.slf4j/slf4j-log4j12 "1.7.1"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring/ring-devel "1.6.3"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]]
  :main ^:skip-aot clinvar-submitter.core
  :ring {:handler clinvar-submitter.web-service/app}
  :plugins [[lein-ring "0.12.1"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
