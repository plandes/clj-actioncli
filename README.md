# Command line interface action oriented framework

This library parses *action based* command line arguments and invokes the
corresponding code.  Action based means using the first token (after the binary
name) as a mnemonic to invoke some action much like `clone` in `git clone`.

Each action mnemonic is mapped to a function with corresponding arguments
passed at invocation time.

This package has a few other basic utility libraries that is used by this
package but useful for many others (i.e. file system path register and
resolution).

# Obtaining

In your `project.clj` file, add:

[![Clojars Project](http://clojars.org/com.zensols.tools/actioncli/latest-version.svg)](http://clojars.org/com.zensols.tools/actioncli/)


# Documentation

Additional documentation:
* [Java](https://plandes.github.io/clj-actioncli/apidocs/index.html)
* [Clojure](https://plandes.github.io/clj-actioncli/codox/index.html)


# Usage

This package supports:
* [Templating](#templating)
* [Action Commands](#action-commands)
* [Resource Location](#resource-location)
* [Executing Action Commands](#executing)


## Templating

You can use the [make project](https://github.com/plandes/clj-mkproj) utility
with the [lein project](https://github.com/plandes/template/tree/master/lein)
directory to template out the files in the following secion.


## Action Commands

Say you're writing a web service (among other things an uberjar might have) and
you want to start it with the command:
```shell
$ java -jar serviceapp.jar service -p 8080
```

There are three kinds of action commands:

* **Global action:** These are parsed first before anything else and usually
  trigger an early exit.  Example options include `--help`, `-h`, `--version`.
* **Single action:** These don't include the name of the action on the command
  line and are typical UNIX like command lines.  Examples include `ls`, `grep`,
  etc.
* **Multi-action:** These include the an action name (think operand) at the
  beginning of the command line.  Examples include `git clone` where `clone` is
  the action name.


Create the following files: service.clj and core.clj
### src/com/example/service.clj
```clojure
(ns com.example.service
  (:require [clojure.tools.logging :as log])
  (:require [zensols.actioncli.parse :refer (with-exception)]))

(defn run-server [port]
  (log/infof "starting service on port %d" port))

(def start-server-command
  {:description "start the guide website and service" 
   :options [["-p" "--port PORT" "the port bind for web site/service"
              :default 8080
              :parse-fn #(Integer/parseInt %)
              :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]]
   :app (fn [opts & args]
          (with-exception
            (run-server (:port opts))))})
```

### src/com/example/core.clj
```clojure
(ns com.example.core
  (:require [zensols.actioncli.parse :as parse]
            [zensols.actioncli.log4j2 :as lu])
  (:require [example.version])
  (:require (base))
  (:gen-class :main true))

(defn- version-info-action []
  (println (format "%s (%s)" example.version/version example.version/gitref)))

(defn- create-action-context []
  (multi-action-context
   '((:service com.example service start-server-action)
     (:repl zensols.actioncli repl repl-action))
   :version-option version-info-action
   :default-arguments ["service" "-p" "8080"]))

(defn -main [& args]
  (lu/configure "service-log4j2.xml")
  (parse/set-program-name "nlpserver")
  (-> (create-action-context)
      (parse/process-arguments args)))
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
user=> (require '[zensols.actioncli.resource :as res])
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

Building
--------
All [leiningen](http://leiningen.org) tasks will work in this project.  For
additional build functionality (git tag convenience utility functionality)
clone the [Clojure build repo](https://github.com/plandes/clj-zenbuild) in the
same (parent of this file) directory as this project:
```bash
   cd ..
   git clone https://github.com/plandes/clj-zenbuild
```

License
--------
Copyright © 2017 Paul Landes

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
