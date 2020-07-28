(ns alc.x-as-tests.impl.validate
  (:require
   [clj-kondo.core :as cc]))

(defn check-source
  [src]
  ;; XXX: would like to avoid temp file...
  ;; XXX: not sure this should always be "clj"
  (let [file-for-clj-kondo (java.io.File/createTempFile "alcxat" ".clj")
        _ (spit file-for-clj-kondo src)
        _ (.deleteOnExit file-for-clj-kondo)
        path (.getAbsolutePath file-for-clj-kondo)
        results-map (cc/run! {:lint [path]})
        num-errs (get-in results-map [:summary :error])]
    (= 0 num-errs)))
