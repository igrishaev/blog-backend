(defproject com.github.igrishaev/blog-backend "0.1.0-SNAPSHOT"

  :description
  "Backend for my blog grishaev.me"

  :url
  "https://github.com/igrishaev/blog-backend"

  :license
  {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
   :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies
  [[org.clojure/clojure "1.11.1"]
   [http-kit "2.6.0"]
   [cheshire "5.10.0"]]

  :main
  ^:skip-aot blog-backend.core

  :jvm-opts ["-Dfile.encoding=UTF-8"]

  :target-path
  "target/uberjar"

  :uberjar-name
  "blog-backend.jar"

  :profiles
  {:dev
   {:global-vars
    {*warn-on-reflection* true
     *assert* true}}

   :uberjar
   {:aot :all
    :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
