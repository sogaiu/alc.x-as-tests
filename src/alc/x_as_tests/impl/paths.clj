(ns alc.x-as-tests.impl.paths
  (:require
   [clojure.java.io :as cji]
   [clojure.string :as cs]))

;; windows 10 PATHEXT default?:
;;   .COM;.EXE;.BAT;.CMD;.VBS;.VBE;.JS;.JSE;.WSF;.WSH;.MSC
(defn which
  [bin-name]
  (let [paths (cs/split (or (System/getenv "PATH") "")
                (re-pattern (java.io.File/pathSeparator)))
        ;; for windows
        pathexts (cs/split (or (System/getenv "PATHEXT") "")
                   (re-pattern (java.io.File/pathSeparator)))]
    ;; adapted work by taylorwood
    (first
      (for [path (distinct paths)
            pathext pathexts
            :let [exe-file (cji/file path (str bin-name pathext))]
            :when (.exists exe-file)]
        (.getAbsolutePath exe-file)))))

(comment

  (which "clojure")
  ;; => "/usr/local/bin/clojure"

  ,)

(defn as-abspath
  [& paths]
  (.getAbsolutePath (apply cji/file paths)))

(comment

  (as-abspath (System/getProperty "user.dir")
              "src")
  #_ (str (System/getProperty "user.dir") "/"
          "src")

  ,)

(defn has-filext?
  [path exts]
  (some #(cs/ends-with? path %)
        exts))

(comment

  (has-filext? (as-abspath (System/getProperty "user.dir")
                           "src" "alc" "x_as_tests"
                           "main.clj")
               #{".clj" ".cljc"})
  ;; => true

  (has-filext? (as-abspath (System/getProperty "user.dir")
                           "src" "alc" "x_as_tests"
                           "main.cljs")
               #{".clj"})
  ;; => nil

  ,)
