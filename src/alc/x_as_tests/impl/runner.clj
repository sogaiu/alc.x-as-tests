(ns alc.x-as-tests.impl.runner
  (:require
   [clojure.java.io :as cji]
   [clojure.string :as cs]))

(defn enum-src-files-in-dir
  [dir]
  (some->> (file-seq (java.io.File. dir))
           (keep (fn [file]
                   (when (.isFile file)
                     (let [path (.getAbsolutePath file)]
                       (when (cs/ends-with? path ".clj")
                         path)))))))

(comment

  (enum-src-files-in-dir
   (.getAbsolutePath (cji/file (System/getenv "HOME")
                               "src" "alc.x-as-tests" "src")))
  #_ (map (fn [path]
            (.getAbsolutePath (cji/file (System/getenv "HOME") path)))
          ["src/alc.x-as-tests/src/alc/x_as_tests/impl/utils.clj"
           "src/alc.x-as-tests/src/alc/x_as_tests/impl/ast.clj"
           "src/alc.x-as-tests/src/alc/x_as_tests/impl/validate.clj"
           "src/alc.x-as-tests/src/alc/x_as_tests/impl/rewrite.clj"
           "src/alc.x-as-tests/src/alc/x_as_tests/impl/runner.clj"
           "src/alc.x-as-tests/src/alc/x_as_tests/main.clj"])

  )

(defn enum-src-files
  [dirs]
  (distinct
   (mapcat enum-src-files-in-dir
           dirs)))

(comment

  (enum-src-files [(.getAbsolutePath (cji/file (System/getenv "HOME")
                                               "src" "alc.x-as-tests" "src"
                                               "alc" "x_as_tests"))
                   (.getAbsolutePath (cji/file (System/getenv "HOME")
                                               "src" "alc.x-as-tests" "src"
                                               "alc" "x_as_tests" "impl"))])
  #_ (map (fn [path]
            (.getAbsolutePath (cji/file (System/getenv "HOME") path)))
          ["src/alc.x-as-tests/src/alc/x_as_tests/impl/utils.clj"
           "src/alc.x-as-tests/src/alc/x_as_tests/impl/ast.clj"
           "src/alc.x-as-tests/src/alc/x_as_tests/impl/validate.clj"
           "src/alc.x-as-tests/src/alc/x_as_tests/impl/rewrite.clj"
           "src/alc.x-as-tests/src/alc/x_as_tests/impl/runner.clj"
           "src/alc.x-as-tests/src/alc/x_as_tests/main.clj"])

  )

(comment

  (defmacro with-out-both
    [& body]
    `(let [s# (new java.io.StringWriter)]
       (binding [*out* s#]
         (let [v# ~@body]
           (vector (str s#)
                   v#)))))

  (defmacro with-test-out-both
    [& body]
    `(let [s# (new java.io.StringWriter)]
       (binding [clojure.test/*test-out* s#]
         (let [v# ~@body]
           (vector (str s#)
                   v#)))))

  )

;; XXX: break this up?
(defn gen-run-schedule
  [paths]
  (cs/join "\n"
           ["(require 'clojure.test)"
            ""
            "(defmacro with-test-out-both"
            "  [& body]"
            "  `(let [s# (new java.io.StringWriter)]"
            "     (binding [clojure.test/*test-out* s#]"
            "       (let [v# ~@body]"
            "         (vector (str s#)"
            "                 v#)))))"
            ""
            "(def summary (atom {}))"
            ""
            "(def line"
            "  \"-----------------------------------------------\")"
            ""
            "(binding [clojure.test/*test-out* *out*]"
       (str "  (doseq [path " (with-out-str (prn paths)) "]")
            "    (swap! summary conj"
            "           [path"
            "            (with-test-out-both"
            "              (load-file path))])))"
            ""
            "(println)"
            ""
            "(doseq [[path [output report]] @summary]"
            "  (println \"path:\" path)"
            "  (when (not= \"nil\\n\" output)"
            "    (println line)"
            "    (println \"output:\")"
            "    (println output))"
            "  (println line)"
            "  (println \"sub:\" report)"
            "  (println line)"
            "  (println))"
            ""
            "(println line)"
            "(println \"total:\""
            "  (let [[test pass fail error]"
            "    (reduce (fn [[t-test t-pass t-fail t-error]"
            "                 {:keys [:test :pass :fail :error]}]"
            "              [(+ t-test test)"
            "               (+ t-pass pass)"
            "               (+ t-fail fail)"
            "               (+ t-error error)])"
            "            [0 0 0 0]"
            "            (map (fn [[_output report]]"
            "              report)"
            "              (vals @summary)))]"
            "    {:test test"
            "     :pass pass"
            "     :fail fail"
            "     :error error}))"
            ""]))

(comment

  (comment

    (print
     (gen-run-schedule [(str (System/getenv "HOME") "/"
                             "src/alc.x-as-tests/fin.clj")
                        (str (System/getenv "HOME") "/"
                             "src/alc.x-as-tests/fun.clj")])
     )

    )

  )
