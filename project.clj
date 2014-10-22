(defproject visule "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[cider/cider-nrepl "0.8.0-snapshot"]] 
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :main visule.core
  :profiles
  {:production
   {}
   :dev
   {;; :source-paths ["dev"]
    :dependencies [[org.clojure/tools.namespace "0.2.5"]]}})
