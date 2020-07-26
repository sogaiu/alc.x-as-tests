(defproject alc.x-as-tests "0.0.1-SNAPSHOT"
  :description "x as tests"
  :url "https://github.com/sogaiu/alc.x-as-tests"
  :source-paths ["src"]
  :dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                 [carocad/parcera "0.11.1"]
                 [org.antlr/antlr4-runtime "4.7.1"]]
  :profiles {:socket-repl {:jvm-opts
                      ["-Dclojure.server.repl={:port 8235 :accept clojure.core.server/repl}"]}
             :uberjar {:global-vars {*assert* false}
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]
                       :aot :all
                       :main alc.x-as_tests.main}}
  :aliases {"alc.xat" ["run" "-m" "alc.x-as-tests.main"]})
