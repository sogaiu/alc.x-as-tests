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
  ;; => "/usr/bin/clojure"

  )
