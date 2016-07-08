(ns ^{:doc "A file system/resource configuration package."
      :author "Paul Landes"}
    zensols.actioncli.resource
  (:use [clojure.pprint :only (pprint)])
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(def ^:private our-ns *ns*)
(def ^:private res-prop-fmt (atom "zensols.%s"))
(def ^:private res-paths (atom {}))

(defn- syspath
  ([name]
   (syspath name name))
  ([name default]
   (let [propstr (System/getProperty (format @res-prop-fmt name))]
     (or propstr default))))

(defn- sysfile
  ([name]
   (sysfile name name))
  ([name default]
   (let [path (syspath name default)]
     (if (= (type path) java.io.File)
       path
       (io/file path)))))

(defn resource-path
  "Get a path to a resource.
  Before using this you must register resources with [[register-resource]].

  ## Keys

  * **:create?** if `true` then create the resource (i.e. director(ies) on the
  file system)"
  ([key child-file & {:keys [create?] :or {:create? nil}}]
   (let [path (io/file (resource-path key) child-file)]
     (case create?
       :file (.mkdirs path)
       :dir (.mkdirs (.getParentFile path))
       :else)
     path))
  ([key]
   (let [path-fn (get @res-paths key)]
     (if (nil? path-fn)
       (throw (ex-info (format "No such resource: %s" key) {:key key})))
     (path-fn))))

(defn set-resource-property-format
  "The format string (used with `format`) to generate the system property for
  resource string lookups in the system properties."
  [fmt-str]
  (reset! res-prop-fmt fmt-str))

(defn register-resource
  "Register a resource to be used with [[resource-path]].
  This should be done at the beginning of the program, usually from `core`.
  **key**: the key to be used for the resource with [[resource-path]].

  ## Keys

  All keys are optional but at least :function or :system-file must be
  provided.

  * **:function** the function to call when the resource is generated
  * **:pre-path** a key of a resource to prepend to the path
  * **:system-file** a file path of the resource--if the same name exists as a
  system property then that is used instead (see [[set-resource-property-format]])
  * **:system-default** the default path to use if the system-file isn't found
  in the system properties"
  [key & {:keys [function pre-path system-file
                 system-default]}]
  (let [fnform (concat (list (or function 'io/file))
                       (if pre-path
                         (list `(resource-path ~pre-path)))
                       (if system-file
                         (list (remove nil? `(sysfile ~system-file
                                                      ~system-default)))))
        ;; bind since calling from other *ns* fails the eval (i.e. requires)
        fval (binding [*ns* our-ns]
               (eval (concat (list 'fn [] fnform))))]
    (swap! res-paths assoc key fval)
    fval))
