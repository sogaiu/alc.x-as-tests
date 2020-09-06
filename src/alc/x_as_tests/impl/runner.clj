(ns alc.x-as-tests.impl.runner
  (:require
   [alc.x-as-tests.impl.paths :as paths]
   [alc.x-as-tests.impl.rewrite :as rewrite]
   [clojure.java.io :as cji]
   [clojure.java.shell :as cjs]
   [clojure.string :as cs]))

(defn enum-src-files-in-dir
  [dir exts]
  (some->> (file-seq (java.io.File. dir))
           (keep (fn [file]
                   (when (.isFile file)
                     (let [path (paths/as-abspath file)]
                       (when (paths/has-filext? path exts)
                         path)))))))

(comment

  (set
   (enum-src-files-in-dir
    (paths/as-abspath (System/getProperty "user.dir")
                      "src")
    #{".clj"}))
  #_ (set
      (map (fn [path]
             (paths/as-abspath (System/getProperty "user.dir")
                               "src" "alc" "x_as_tests"
                               path))
           ["impl/ast.clj"
            "impl/paths.clj"
            "impl/rewrite.clj"
            "impl/runner.clj"
            "impl/utils.clj"
            "impl/validate.clj"
            "main.clj"]))

  )

(defn all-src-files
  [paths exts]
  (reduce (fn [acc path]
            (let [f (cji/file path)]
              (cond
                (.isFile f)
                (conj acc path)
                ;;
                (.isDirectory f)
                (into acc (vec (enum-src-files-in-dir path exts)))
                ;;
                :else
                (throw (Exception. (str "not file or dir: " path))))))
          []
          paths))

(comment

  (set
   (all-src-files
    [(paths/as-abspath (System/getProperty "user.dir")
                       "src" "alc" "x_as_tests" "main.clj")
     (paths/as-abspath (System/getProperty "user.dir")
                       "src" "alc" "x_as_tests" "impl")]
    #{".clj"}))
  #_ (->> (cji/file (System/getProperty "user.dir")
                    "src")
          file-seq
          (filter #(.isFile %))
          (map #(.getAbsolutePath %))
          set)

  )

(defn gen-test-path
  [full-path root-path test-root]
  (let [nio-full-path
        (java.nio.file.Paths/get full-path
                                 (into-array String []))
        nio-root-path
        (java.nio.file.Paths/get root-path
                                 (into-array String []))]
    (paths/as-abspath test-root
                      (-> (.relativize nio-root-path
                                       nio-full-path)
                          .toString))))

(comment

  (let [proj-root (System/getProperty "user.dir")]
    (gen-test-path (paths/as-abspath proj-root
                                     "src" "alc" "x_as_tests"
                                     "main.clj")
                   (paths/as-abspath proj-root
                                     "src")
                   (System/getProperty "java.io.tmpdir")))
  #_ (paths/as-abspath (System/getProperty "java.io.tmpdir")
                       "alc" "x_as_tests"
                       "main.clj")

  )

;; XXX: generates test file paths and populates the corr files
(defn gen-tests!
  [paths relative-to test-root]
  (->> paths
       (map (fn [path]
              (let [test-path (gen-test-path path relative-to test-root)
                    _ (cji/make-parents test-path)
                    src (slurp path)]
                (spit test-path
                      (rewrite/rewrite-with-tests src))
                test-path)))
       doall))

(comment

  ;; XXX: debris ends up in /tmp -- clean up?
  (let [proj-root (System/getProperty "user.dir")]
    (gen-tests!
     [(paths/as-abspath proj-root
                        "src" "alc" "x_as_tests"
                        "main.clj")]
     (paths/as-abspath proj-root
                       "src")
     (paths/as-abspath (System/getProperty "java.io.tmpdir")
                       "alc.x-as-tests" "src")))
  #_ [(paths/as-abspath (System/getProperty "java.io.tmpdir")
                        "alc.x-as-tests"
                        "src" "alc" "x_as_tests"
                        "main.clj")]

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

  (print
   (gen-run-schedule [(paths/as-abspath (System/getProperty "user.dir")
                                        "fin.clj")
                      (paths/as-abspath (System/getProperty "user.dir")
                                        "fun.clj")])
   )

  )

(defn do-tests!
  [{:keys [:exe-name
           :exts
           :paths
           :temp-root
           :verbose]
    :or {exe-name (or (System/getenv "ALC_XAT_CLJ_NAME")
                      "clojure") ;; don't include file extension
         exts #{".clj" ".cljc"}
         paths [(paths/as-abspath (System/getProperty "user.dir")
                                  "src")]
         temp-root (paths/as-abspath (System/getProperty "java.io.tmpdir")
                                     (str "alc.x-as-tests-"
                                          (System/currentTimeMillis)))}}]
  (if-let [clojure-bin (paths/which exe-name)]
    (let [paths (all-src-files paths exts)
          proj-root (System/getProperty "user.dir")
          test-paths (gen-tests! paths proj-root temp-root)
          runner-path (paths/as-abspath temp-root
                                        "alc.xat.run-tests.clj")
          _ (spit runner-path (gen-run-schedule test-paths))
          cmd [clojure-bin "-i" runner-path]
          {:keys [:err :exit :out]}
          (cjs/with-sh-dir proj-root
            (apply cjs/sh cmd))]
      (when verbose
        (println cmd))
      (when (not= 0 exit)
        (println cmd)
        (println "  exit:" exit)
        (println "  err:" err))
      (println out))
    (do
      (println "Failed to find clojure executable:" exe-name)
      (flush)
      (System/exit 1))))

(comment

  (do-tests! {:verbose true})

  (let [impl-dir (paths/as-abspath (System/getProperty "user.dir")
                                   "src" "alc" "x_as_tests" "impl")]
    (do-tests! {:verbose true
                :paths [(paths/as-abspath impl-dir "ast.clj")
                        (paths/as-abspath impl-dir "rewrite.clj")]}))

  )
