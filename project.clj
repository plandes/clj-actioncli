(defproject com.zensols.tools/actioncli "0.1.0-SNAPSHOT"
  :description "An action oriented framework to the CLI (and various other libraries)."
  :url "https://github.com/plandes/clj-actionclj"
  :license {:name "Apache License version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0"
            :distribution :repo}
  :plugins [[lein-codox "0.10.3"]
            [lein-javadoc "0.3.0"]
            [org.clojars.cvillecsteele/lein-git-version "1.2.7"]]
  :codox {:metadata {:doc/format :markdown}
          :project {:name "Action CLI"}
          :output-path "target/doc/codox"
          :source-uri "https://github.com/plandes/clj-actioncli/blob/v{version}/{filepath}#L{line}"}
  :javadoc-opts {:package-names ["com.zensols.log"]
                 :output-dir "target/doc/apidocs"}
  :git-version {:root-ns "zensols.actioncli"
                :path "src/clojure/zensols/actioncli"
                :version-cmd "git describe --match v*.* --abbrev=4 --dirty=-dirty"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :javac-options ["-Xlint:unchecked"]
  :jar-exclusions [#".gitignore"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 
                 ;; command line
                 [org.clojure/tools.cli "0.3.5"]

                 ;; repl
                 [org.clojure/tools.nrepl "0.2.12"]

                 ;; logging
                 [org.clojure/tools.logging "0.3.1"]

                 ;; sandboxing - timeout
                 [clojail "1.0.6"]]
  :profiles {:provided
             {:dependencies [[org.apache.logging.log4j/log4j-core "2.7"]]}
             :snapshot {:git-version {:version-cmd "echo -snapshot"}}
             :appassem {:aot :all}
             :test
             {:jvm-opts ["-Dlog4j.configurationFile=test-resources/log4j2.xml"
                         "-Xms4g" "-Xmx12g" "-XX:+UseConcMarkSweepGC"]}})
