(ns alc.x-as-tests.impl.ex)

(def msg-const
  "ALC_XAT_THROW")

(defn throw-info
  [m]
  (throw
   (ex-info msg-const m)))
