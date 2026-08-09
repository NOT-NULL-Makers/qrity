(ns build
  "Release build: clojure -T:build jar | install | clean.

  Produces a source jar (the library ships as .cljc/.clj/.cljs sources;
  consumers' own builds compile them) with the pom Clojars requires.
  Deployment goes through the :deploy alias with a Clojars token in
  CLOJARS_USERNAME/CLOJARS_PASSWORD; deploying is a maintainer action."
  (:require [clojure.tools.build.api :as b]))

(def lib 'com.notnullmakers/qrity)
(def version "0.1.0")
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(def ^:private basis
  (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (b/write-pom
   {:class-dir class-dir
    :lib lib
    :version version
    :basis @basis
    :src-dirs ["src"]
    :scm {:url "https://github.com/NOT-NULL-Makers/qrity"
          :connection "scm:git:git://github.com/NOT-NULL-Makers/qrity.git"
          :developerConnection
          "scm:git:ssh://git@github.com/NOT-NULL-Makers/qrity.git"
          :tag (str "v" version)}
    :pom-data
    [[:description
      (str "An experimental from-scratch QR Code generator and reader "
           "for Clojure and ClojureScript, implemented against "
           "ISO/IEC 18004:2015 with a pure core and thin platform "
           "pixel adapters. No conformance claim.")]
     [:url "https://github.com/NOT-NULL-Makers/qrity"]
     [:licenses
      [:license
       [:name "MIT License"]
       [:url "https://opensource.org/license/mit"]]]]})
  (b/copy-dir {:src-dirs ["src"] :target-dir class-dir})
  (b/copy-file {:src "LICENSE" :target (str class-dir "/LICENSE")})
  (b/copy-file {:src "README.md" :target (str class-dir "/README.md")})
  (b/jar {:class-dir class-dir :jar-file jar-file})
  ;; deps-deploy reads pom.xml from the working directory, not the jar.
  (b/copy-file {:src (b/pom-path {:class-dir class-dir :lib lib})
                :target "pom.xml"})
  (println "built" jar-file))

(defn install [_]
  (jar nil)
  (b/install {:basis @basis
              :lib lib
              :version version
              :jar-file jar-file
              :class-dir class-dir})
  (println "installed" (str lib) version))
