(ns alc.x-as-tests.impl.validate
  (:require
   [clj-kondo.core :as cc]))

(defn check-source
  [src]
  (let [results-map (with-in-str src
                      (cc/run! {:lint ["-"]}))
        num-errs (get-in results-map [:summary :error])]
    (= 0 num-errs)))