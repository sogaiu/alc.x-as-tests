(defproject alc.x-as-tests "0.0.1-SNAPSHOT"
  :description "x as tests"
  :url "https://github.com/sogaiu/alc.x-as-tests"
  :source-paths ["src"]
  :dependencies [[carocad/parcera "0.11.3"]
                 [clj-kondo "2020.07.29"]
                 [org.antlr/antlr4-runtime "4.7.1"]
                 ;; parcera appears to need >= 1.10.x
                 [org.clojure/clojure "1.10.1"]]
  :profiles {:socket-repl
             {:jvm-opts
              ["-Dclojure.server.repl={:port 8235
                                       :accept clojure.core.server/repl}"]}
             ;; see script/compile
             :clojure-1.10.2-alpha1
             {:dependencies [[org.clojure/clojure "1.10.2-alpha1"]]}
             ;; see script/compile
             :native-image
             {:dependencies
              [[borkdude/clj-reflector-graal-java11-fix "0.0.1-graalvm-20.2.0"]
               [borkdude/sci.impl.reflector "0.0.1-java11"]]}
             ;;
             :uberjar {;;:global-vars {*assert* true}
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]
                       :aot :all
                       :main alc.x-as_tests.main}}
  :aliases {"alc.xat" ["run" "-m" "alc.x-as-tests.main"]})
