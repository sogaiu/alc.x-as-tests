(ns alc.x-as-tests.impl.runner
  (:require
   [alc.x-as-tests.impl.paths :as paths]
   [alc.x-as-tests.impl.rewrite :as rewrite]
   [clojure.java.io :as cji]
   [clojure.java.shell :as cjs]
   [clojure.string :as cs]))

(defn enum-src-files-in-dir
  [dir]
  (some->> (file-seq (java.io.File. dir))
           (keep (fn [file]
                   (when (.isFile file)
                     (let [path (.getAbsolutePath file)]
                       ;; XXX: don't hard-wire?
                       (when (cs/ends-with? path ".clj")
                         path)))))))

(comment

  (set (enum-src-files-in-dir
        (.getAbsolutePath (cji/file (System/getenv "HOME")
                                    "src" "alc.x-as-tests" "src"))))
  #_ (set
      (map (fn [path]
             (.getAbsolutePath (cji/file (System/getenv "HOME") path)))
           ["src/alc.x-as-tests/src/alc/x_as_tests/impl/utils.clj"
            "src/alc.x-as-tests/src/alc/x_as_tests/impl/ast.clj"
            "src/alc.x-as-tests/src/alc/x_as_tests/impl/validate.clj"
            "src/alc.x-as-tests/src/alc/x_as_tests/impl/paths.clj"
            "src/alc.x-as-tests/src/alc/x_as_tests/impl/rewrite.clj"
            "src/alc.x-as-tests/src/alc/x_as_tests/impl/runner.clj"
            "src/alc.x-as-tests/src/alc/x_as_tests/main.clj"]))

  )

(defn src-files-map
  [dir]
  (into {}
        (map (fn [path]
               [path dir])
             (enum-src-files-in-dir dir))))

(comment

  (src-files-map
   (.getAbsolutePath (cji/file (System/getenv "HOME")
                               "src" "alc.x-as-tests" "src")))
  #_ (let [root-path
           (.getAbsolutePath (cji/file (System/getenv "HOME")
                                       "src" "alc.x-as-tests" "src"))]
       (into {}
             (map #(vector
                    (.getAbsolutePath (cji/file root-path %))
                    root-path)
                  ["alc/x_as_tests/impl/utils.clj"
                   "alc/x_as_tests/impl/ast.clj"
                   "alc/x_as_tests/impl/validate.clj"
                   "alc/x_as_tests/impl/paths.clj"
                   "alc/x_as_tests/impl/rewrite.clj"
                   "alc/x_as_tests/impl/runner.clj"
                   "alc/x_as_tests/main.clj"])))

  )

(defn all-src-files
  [dirs]
  (apply merge
         (map src-files-map
              dirs)))

(comment

  (all-src-files
   [(.getAbsolutePath (cji/file (System/getenv "HOME")
                                "src" "alc.x-as-tests" "src"
                                "alc" "x_as_tests"))
    (.getAbsolutePath (cji/file (System/getenv "HOME")
                                "src" "alc.x-as-tests" "src"
                                "alc" "x_as_tests" "impl"))])
  #_ (let [one-root
           (.getAbsolutePath (cji/file (System/getenv "HOME")
                                       "src" "alc.x-as-tests" "src"
                                       "alc" "x_as_tests"))
           another-root
           (.getAbsolutePath (cji/file (System/getenv "HOME")
                                       "src" "alc.x-as-tests" "src"
                                       "alc" "x_as_tests" "impl"))]
       (into {}
             (concat (map #(vector
                            (.getAbsolutePath (cji/file another-root %))
                            another-root)
                          ["utils.clj"
                           "ast.clj"
                           "validate.clj"
                           "paths.clj"
                           "rewrite.clj"
                           "runner.clj"])
                     (map #(vector
                            (.getAbsolutePath (cji/file one-root %))
                            one-root)
                          ["main.clj"]))))

  )

(defn gen-test-path
  [full-path root-path test-root]
  (let [nio-full-path
        (java.nio.file.Paths/get full-path
                                 (into-array String []))
        nio-root-path
        (java.nio.file.Paths/get root-path
                                 (into-array String []))]
    (.getAbsolutePath (cji/file test-root
                                (-> (.relativize nio-root-path
                                                 nio-full-path)
                                    .toString)))))

(comment

  (gen-test-path (.getAbsolutePath (cji/file (System/getenv "HOME")
                                             "src" "alc.x-as-tests"
                                             "src" "alc" "x_as_tests"
                                             "main.clj"))
                 (.getAbsolutePath (cji/file (System/getenv "HOME")
                                             "src" "alc.x-as-tests"))
                 "/tmp")
  ;; => "/tmp/src/alc/x_as_tests/main.clj"


  )

;; XXX: generates test file paths and populates the corr files
(defn gen-tests!
  [paths-map test-root]
  (->> paths-map
       (map (fn [[path dir]]
              (let [test-path (gen-test-path path dir test-root)
                    _ (cji/make-parents test-path)
                    src (slurp path)]
                (spit test-path
                      (rewrite/rewrite-with-tests src))
                test-path)))
       doall))

(comment

  ;; XXX: debris ends up in /tmp -- clean up?
  (gen-tests!
   (let [one-root
         (.getAbsolutePath (cji/file (System/getenv "HOME")
                                     "src" "alc.x-as-tests" "src"
                                     "alc" "x_as_tests"))]
     {(.getAbsolutePath (cji/file (System/getenv "HOME")
                                  "src" "alc.x-as-tests" "src"
                                  "alc" "x_as_tests"
                                  "main.clj"))
      one-root})
   "/tmp")
  ;; => '("/tmp/main.clj")

  ;; XXX: debris ends up in /tmp -- clean up?
  (gen-tests!
   (let [one-root
         (.getAbsolutePath (cji/file (System/getenv "HOME")
                                     "src" "alc.x-as-tests" "src"))]
     {(.getAbsolutePath (cji/file (System/getenv "HOME")
                                  "src" "alc.x-as-tests" "src"
                                  "alc" "x_as_tests"
                                  "main.clj"))
      one-root})
   "/tmp")
  ;; => '("/tmp/alc/x_as_tests/main.clj")

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
       (str "  (doseq [path " (with-out-str (prn (vec paths))) "]")
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

;; XXX: support filepaths too?
(defn do-tests!
  [{:keys [:dirs
           :temp-root]
    :or {dirs [(.getAbsolutePath
                (cji/file (System/getProperty "user.dir") "src"))]
         temp-root (System/getProperty "java.io.tmpdir")}}]
  (assert (paths/which "clojure") "Failed to locate clojure")
  (let [paths-map (all-src-files dirs)
        test-paths (gen-tests! paths-map temp-root)
        proj-root (System/getProperty "user.dir")
        ;; XXX: need temp name
        schedule-path (.getAbsolutePath
                       (cji/file proj-root "alc.xat.schedule.clj"))
        _ (spit schedule-path (gen-run-schedule test-paths))
        {:keys [:err :exit :out]}
        (cjs/with-sh-dir (System/getenv "user.dir")
          ;; XXX: existence check for "clojure" earlier?
          (cjs/sh "clojure" "-i" schedule-path))]
    (println "err:" err)
    (println "exit:" exit)
    ;; XXX: could check err and exit...
    (println "out:" out)))

(comment

  (do-tests! [(.getAbsolutePath (cji/file (System/getenv "HOME")
                                          "src" "alc.x-as-tests" "src"))])

  )
