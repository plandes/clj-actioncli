(defproject com.zensols.tools/actioncli "0.1.0-SNAPSHOT"
  :description "An action oriented framework to the CLI (and various other libraries)."
  :url "https://github.com/plandes/clj-actionclj"
  :license {:name "Apache License version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :plugins [[lein-codox "0.10.0"]
            [org.clojars.cvillecsteele/lein-git-version "1.0.3"]]
  :codox {:metadata {:doc/format :markdown}
          :project {:name "Action CLI"}
          :output-path "target/doc/codox"}
  :source-paths ["src/clojure"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 
                 ;; command line
                 [org.clojure/tools.cli "0.3.5"]

                 ;; repl
                 [org.clojure/tools.nrepl "0.2.12"]

                 ;; logging
                 [org.apache.logging.log4j/log4j-core "2.3"]
                 [org.clojure/tools.logging "0.3.1"]]
  :profiles {:appassem {:aot :all}
             :dev
             {:jvm-opts
              ["-Dlog4j.configurationFile=test-resources/log4j2.xml" "-Xms4g" "-Xmx12g" "-XX:+UseConcMarkSweepGC"]
              :dependencies [[com.zensols/clj-append "1.0.4"]]}})
