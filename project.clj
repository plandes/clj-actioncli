(defproject com.zensols.tools/actioncli "0.0.1"
  :description "An action oriented framework to the CLI (and various other libraries)."
  :url "https://github.com/plandes/clj-actionclj"
  :license {:name "Apache License version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :pom-addition [:developers [:developer {:id "plandes"}
                              [:name "Paul Landes"]
                              [:url "https://github.com/plandes"]]]
  :plugins [[lein-codox "0.9.5"]]
  :codox {:metadata {:doc/format :markdown}
          :project {:name "Action CLI"}
          :output-path "target/doc/codox"}
  :source-paths ["src/clojure"]
  :dependencies [[org.clojure/clojure "1.8.0"]

                 ;; command line
                 [org.clojure/tools.cli "0.3.5"]

                 ;; repl
                 [org.clojure/tools.nrepl "0.2.11"]

                 ;; logging
                 [org.apache.logging.log4j/log4j-core "2.3"]
                 [org.clojure/tools.logging "0.3.1"]]
  :aot :all)
