Provide an action oriented framework to the CLI (and various other libraries).
==============================================================================

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

```clojure
[com.zensols.tools/actioncli "0.0.1"]
```

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

# Resource Location

Many apps (command line) need to find paths on the file system.  This library
provides a way to both register and refine those locations with Java system
properties.

```clojure
(:require '[zensols.actioncli.resource :as res])

(res/register-resource :data :system-file "data" :system-default "../data")
(res/register-resource :runtime-gen :pre-path :data :system-file "db")

(.getPath (res/resource-path :data))
 => ../data

(.getPath (res/resource-path :runtime-gen))
 => ../data/db

(res/set-resource-property-format "myapp.%s")

(System/setProperty "myapp.data" "/new-data-path")

(.getPath (res/resource-path :data))
 => ../new-data-path

(.getPath (res/resource-path :runtime-gen))
 => ../new-data-path/db
```

Documentation
-------------
Additional documentation:
* [Java](https://plandes.github.io/clj-actioncli/apidocs/index.html)
* [Clojure](https://plandes.github.io/clj-actioncli/codox/index.html)

Licencse
--------
Copyright © 2016 Paul Landes

Apache License version 2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
