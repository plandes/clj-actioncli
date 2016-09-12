(ns ^{:doc "A file system/resource configuration package."
      :author "Paul Landes"}
    zensols.actioncli.resource
  (:use [clojure.pprint :only (pprint)])
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]))

(def ^:private our-ns *ns*)
(def ^:private res-prop-fmt (atom "zensols.%s"))
(def ^:private res-paths (atom {}))

(defn- sysprop [name & {:keys [throw?] :or {throw? true}}]
  (let [val (System/getProperty (format @res-prop-fmt name))]
    (if (and (nil? val) throw?)
      (throw (ex-info (format "System property '%s' is unset" name)
                      {:name name})))
    val))

(defn- sysdefault
  ([name]
   (sysdefault name name))
  ([name default]
   (let [propstr (sysprop name :throw? false)]
     (or propstr default))))

(defn- sysfile
  ([name]
   (sysfile name name))
  ([name default]
   (let [path (sysdefault name default)]
     (if (= (type path) java.io.File)
       path
       (io/file path)))))

(defn resource-path
  "Get a path to a resource.
  Before using this you must register resources with [[register-resource]].

  ## Keys

  * **:create** if `:file` then create the director(ies) on the
  file system, otherwise if `:dir` then create all parent directories"
  ([key child-file & {:keys [create] :or {:create nil}}]
   (let [path (io/file (resource-path key) child-file)]
     (case create
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

(defn- resource-to-path
  ([prepath path]
   (let [prepath (cond (instance? java.io.File prepath) (.getPath prepath)
                       (instance? java.net.URL prepath) (.getFile prepath)
                       true prepath)]
     (if path
       (str prepath "/" path)
       prepath))))

(defn- resolve-resource
  ([type prepath]
   (resolve-resource type prepath nil))
  ([type prepath path]
   (case type
     :file (if (instance? java.io.File prepath)
             (if path
               (io/file prepath path)
               prepath)
             (if path
               (io/file (resource-to-path prepath nil) path)
               (io/file (resource-to-path prepath nil))))
     :resource (io/resource (resource-to-path prepath path)))))

(defn register-resource
  "Register a resource to be used with [[resource-path]].
  This should be done at the beginning of the program, usually from `core`.
  **key**: the key to be used for the resource with [[resource-path]].

  ## Keys

  All keys are optional but at least :function or :system-file must be
  provided.

  * **:function** the function to call when the resource is generated
  * **:type** the type of resource to return; one of `:resource` or `:file` (default)
  * **:pre-path** a key of a resource to prepend to the path
  * **:system-file** a file path of the resource--if the same name exists as a
  system property then that is used instead (see [[set-resource-property-format]])
  * **:system-default** the default path to use if the system-file isn't found
  in the system properties
  * **:system-property** get the resource directly from the system properties"
  [key & {:keys [function pre-path constant type
                 system-file system-property system-default]
          :or {type :file}}]
  (let [fnform (concat (list (or function 'resolve-resource) type)
                       (if pre-path
                         (list `(resource-path ~pre-path)))
                       (if constant
                         (list constant))
                       (if system-property
                         (list `(sysdefault ~system-property)))
                       (if system-file
                         (list (remove nil? `(sysfile ~system-file
                                                      ~system-default)))))
        _ (log/debugf "registering form: %s" (pr-str fnform))
        ;; bind since calling from other *ns* fails the eval (i.e. requires)
        fval (binding [*ns* our-ns]
               (eval (concat (list 'fn [] fnform))))]
    (swap! res-paths assoc key fval)
    fval))
