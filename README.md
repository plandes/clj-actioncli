An action oriented framework to the CLI (and various other libraries)
=====================================================================

This library parses *action based* command line arguments and invokes the
corresponding code.  Action based means using the first token (after the binary
name) as a mnemonic to invoke some action much like `clone` in `git clone`.

Each action mnemonic is mapped to a function with corresponding arguments
passed at invocation time.

This package has a few other basic utility libraries that is used by this
package but useful for many others (i.e. file system path register and
resolution).

Obtaining
---------
For Clojure, in your `project.clj` file, add:

[![Clojars Project](http://clojars.org/com.zensols.tools/actioncli/latest-version.svg)](http://clojars.org/com.zensols.tools/actioncli/)

For Java, in your `pom.xml` file, add:
```xml
<repositories>
    <repository>
        <id>clojars</id>
        <url>http://clojars.org/repo/</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.zensols.tools</groupId>
    <artifactId>actioncli</artifactId>
    <version>0.0.1</version>
</dependency>
```

Usage
-----
This package supports:
* [Action Commands](#action-commands)
* [Resource Location](#resource-location)
* [Executing Action Commands](#executing)

# Action Commands
Say you're writing a web service (among other things an uberjar might have) and
you want to start it with the command:
```shell
$ java -jar serviceapp.jar service -p 8080
```

Create the following files: service.clj and core.clj
### src/com/example/service.clj
```clojure
(ns com.example.service
  (:require [clojure.tools.logging :as log]))

(defn run-server [port]
  (log/infof "starting service on port %d" port))

(def start-server-command
  {:description "start the guide website and service" 
   :options [["-p" "--port PORT" "the port bind for web site/service"
              :default 8080
              :parse-fn #(Integer/parseInt %)
              :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]]
   :app (fn [opts & args]
          (run-server (:port opts)))})
```

### src/com/example/core.clj
```clojure
(ns taskmatch.service.core
  (:require [clojure.string :as str])
  (:require [taskmatch.base.cli :as cli]
            [taskmatch.base.log4j-util :as lu]
            [taskmatch.base.db :as db])
  (:require (base version))
  (:gen-class :main true))

(def ^:private version-info-command
  {:description "Get the version of the application."
   :options [["-g" "--gitref"]]
   :app (fn [{refp :gitref} & args]
          (println base.version/version)
          (if refp (println base.version/gitref)))})

(def ^:private command-defs
  '((:dbinfo taskmatch.base db db-info-command)
    (:service taskmatch.service agent-handler start-server-command)))

(defn- create-command-context []
  {:command-defs command-defs
   :single-commands {:version version-info-command}
   :default-arguments ["service" "-p" "8080"]})

(defn -main [& args]
  (lu/configure "service-log4j2.xml")
  (let [command-context (create-command-context)]
    (apply cli/process-arguments command-context args)))
```

### resources/service-log4j.xml
```xml
<configuration status="OFF">
    <appenders>
        <console name="console" target="SYSTEM_OUT">
            <patternLayout pattern="%c{1}: %m%n"/>
        </console>
    </appenders>
    <loggers>
        <logger name="com.example" level="info"/>
        <root level="warn">
            <appenderRef ref="console"/>
        </root>
    </loggers>
</configuration>
```
## Executing
```bash
$ java -jar target/clj-actioncli-example-0.1.0-SNAPSHOT-standalone.jar --help
service	start the guide website and service
  -p, --port PORT  8080  the port bind for web site/service

 repl	start a repl either on the command line or headless with -h
  -h, --headless  start an nREPL server
  -p, --port      the port bind for the repl server

 version	Get the version of the application.
  -g, --gitref

$ java -jar exampleapp-standalone.jar version
0.0.1
$ java -jar exampleapp-standalone.jar version -g
0.0.1
<some git ref>
$ java -jar exampleapp-standalone.jar service -p 1234
Jul 08, 2016 6:09:08 PM clojure.tools.logging$eval1$fn__5 invoke
INFO: starting service on port 1234
$ java -jar exampleapp-standalone.jar repl
network-repl
Clojure 1.8.0
user=> (+ 1 1)
2
```

# Resource Location
Many apps (command line) need to find paths on the file system.  This library
provides a way to both register and refine those locations with Java system
properties.

```clojure
user=> (:require '[zensols.actioncli.resource :as res])
user=> (res/register-resource :data :system-file "data" :system-default "../data")
#function[zensols.actioncli.resource/eval9492/fn--9493]
user=> (res/register-resource :runtime-gen :pre-path :data :system-file "db")
#function[zensols.actioncli.resource/eval9498/fn--9499]
user=> (.getPath (res/resource-path :data))
../data
user=> (.getPath (res/resource-path :runtime-gen))
../data/db
user=> (res/set-resource-property-format "myapp.%s")
"myapp.%s"
user=> (System/setProperty "myapp.data" "/new-data-path")
nil
user=> (.getPath (res/resource-path :data))
../new-data-path
user=> (.getPath (res/resource-path :runtime-gen))
../new-data-path/db
```

Documentation
-------------
Additional [documentation](https://plandes.github.io/clj-actioncli/codox/index.html).

Licencse
--------
Copyright © 2016 Paul Landes

Apache License version 2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
