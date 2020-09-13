(ns alc.x-as-tests.impl.utils)

(defn last-not
  [coll pred]
  (->> (reverse coll)
       (filter (fn [elt]
                 (not (pred elt))))
       first))

(comment

  (last-not [1 2 3 7 8 10 12] even?)
  ;; => 7

  ,)

(defn splice-after-vec
  [xs pred ys]
  (let [[almost-before [to-move & after]] (split-with #(not (pred %)) xs)
        before (conj (vec almost-before) to-move)]
    (-> (into before ys)
        (into after))))

(comment

  (splice-after-vec [0 1 2 3 4 5 6 7 8 9]
                    #(= % 5)
                    [5.25 5.5 5.75])
  ;; => [0 1 2 3 4 5 5.25 5.5 5.75 6 7 8 9]

  (splice-after-vec '(0 1 2 3 4 5 6 7 8 9)
                    #(= % 5)
                    '(5.25 5.5 5.75))
  ;; => [0 1 2 3 4 5 5.25 5.5 5.75 6 7 8 9]

  ,)
