(ns alc.x-as-tests.impl.validate
  (:require
   [alc.x-as-tests.impl.ex :as ex]
   [clj-kondo.core :as cc]
   [clojure.string :as cs]))

(defn check-source
  [src]
  (let [results-map (with-in-str src
                      (cc/run! {:lint ["-"]}))
        num-errs (get-in results-map [:summary :error])]
    (when (< 0 num-errs)
      (filter (fn [{:keys [:level]}]
                (= level :error))
              (:findings results-map)))))

(defn do-it
  [source-str]
  (when-let [findings (check-source source-str)]
    (ex/throw-info {:err-msg
                    (str "Errors detected in source:\n"
                         (cs/join "\n"
                                  (map (fn [{:keys [message row]}]
                                         (str "  row:" row " - "
                                              message))
                                       findings)))})))
