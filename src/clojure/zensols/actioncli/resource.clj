(ns ^{:doc "A file system/resource configuration package."
      :author "Paul Landes"}
    zensols.actioncli.resource
  (:import [java.io File]
           [java.net URL])
  (:require [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.test :refer (function?)]))

(def ^:private our-ns *ns*)

(def ^:private res-prop-fmt (atom "zensols.%s"))

(defonce ^{:dynamic true
           :doc "Meant for this namespace internal use only."}
  *resource-paths*
  (atom {}))

(defmacro with-resources
  "Allows for temporal registration and path rendering in a lexical context."
  {:style/indent 0}
  [& forms]
  `(let [curr-res-paths# (deref *resource-paths*)]
     (binding [*resource-paths* (atom curr-res-paths#)]
       ~@forms)))

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
     (if (= (type path) File)
       path
       (io/file path)))))

(defn- resource-to-path
  ([restype prepath path]
   (log/debugf "resource to path: %s, %s (%s), %s"
               restype prepath (type prepath) path)
   (let [prepath-url? (instance? URL prepath)
         prepath (cond (instance? File prepath) (.getPath prepath)
                       prepath-url?
                       (case restype
                         :file (.getFile prepath)
                         :resource (.toString prepath))

                       true prepath)
         catpath (if path
                   (str prepath "/" path)
                   prepath)]
     (log/debugf "catpath: %s, restype: %s" catpath restype)
     (case restype
       :file catpath
       :resource (if prepath-url?
                   (URL. catpath)
                   (io/resource catpath))))))

(defn- resolve-resource
  ([type prepath]
   (resolve-resource type prepath nil))
  ([type prepath path]
   (case type
     :function (prepath path)
     :file (if (instance? File prepath)
             (if path
               (io/file prepath path)
               prepath)
             (if path
               (io/file (resource-to-path :file prepath nil) path)
               (io/file (resource-to-path :file prepath nil))))
     :resource (resource-to-path :resource prepath path)
     :constant prepath)))

(defn- resolve-function [key apply?]
  (let [path-fn (get (deref *resource-paths*) key)]
    (if (nil? path-fn)
      (-> (format "No such resource: %s" key)
          (ex-info {:key key})
          throw))
    (let [path-or-fn (path-fn)]
      (if (and apply? (function? path-or-fn))
        (path-or-fn)
        path-or-fn))))

(defn resource-path
  "Get a path to a resource.
  Before using this you must register resources with [[register-resource]].

  ## Keys

  * **:create** if `:file` then create the director(ies) on the
  file system, otherwise if `:dir` then create all parent directories"
  ([key child-file & {:keys [create] :or {create nil}}]
   (let [parent (resolve-function key false)
         path (resolve-resource (cond (instance? File parent) :file
                                      (function? parent) :function
                                      (instance? java.net.URL parent) :resource
                                      (string? parent) :resource
                                      true :constant)
                                parent child-file)]
     (case create
       :file (.mkdirs path)
       :dir (.mkdirs (.getParentFile path))
       :else)
     path))
  ([key]
   (resolve-function key true)))

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

  * **:function** the function to call when the resource is generated (see
  the [resource
  test](https://github.com/plandes/clj-actioncli/blob/master/test/zensols/actioncli/resource_test.clj)
  and example of how to use this)
  * **:type** the type of resource to return; one of `:resource` or
    `:file` (default); **Imporant**: when using `:resource` use the `:constant`
     keyword for the value
  * **:pre-path** a key of a resource to prepend to the path
  * **:system-file** a file path of the resource--if the same name exists as a
  system property then that is used instead (see [[set-resource-property-format]])
  * **:system-default** the default path to use if the system-file isn't found
  in the system properties
  * **:system-property** get the resource directly from the system properties"
  [key & {:keys [function pre-path constant type
                 system-file system-property system-default]
          :or {type :file}}]
  (let [fnform (concat (if function
                         [function type]
                         ['resolve-resource type])
                       (if pre-path
                         (list `(resource-path ~pre-path)))
                       (if constant
                         (list constant))
                       (if system-property
                         (list `(sysdefault ~system-property)))
                       (if system-file
                         (list (remove nil? `(sysfile ~system-file
                                                      ~system-default)))))
        _ (log/debugf "registering form: %s -> %s" key (pr-str fnform))
        ;; bind since calling from other *ns* fails the eval (i.e. requires)
        fval (binding [*ns* our-ns]
               (if function
                 (constantly (first fnform))
                 (eval (concat (list 'fn [] fnform)))))]
    (swap! *resource-paths* assoc key fval)
    (log/tracef "resource-path: %s -> %s" key (resource-path key))
    fval))
