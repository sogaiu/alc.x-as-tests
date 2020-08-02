(ns alc.x-as-tests.impl.rewrite.core
  (:require
   [alc.x-as-tests.impl.ast :as ast]
   [alc.x-as-tests.impl.utils :as utils]
   [clojure.string :as cs]))

(defn last-non-whitespace
  [nodes]
  (utils/last-not nodes ast/whitespace?))

(comment

  (last-non-whitespace
   '((:whitespace " ")
     (:number "1")))
  ;; => [:number "1"]

  (last-non-whitespace
   '((:keyword ":b")
     (:whitespace "\n")))
  ;; => [:keyword ":b"]

  (last-non-whitespace '())
  ;; => nil

  )

(defn rewrite-expected
  [node]
  (cond
    (ast/line-comment-with-expected? node)
    (ast/expected-from-line-comment node)
    ;;
    (ast/discard-with-form? node)
    (ast/expected-from-discard node)
    ;;
    :else
    nil))

(comment

  (rewrite-expected '(:comment ";; => 2"))
  ;; => [:number "2"]

  (rewrite-expected '(:discard
                      (:whitespace " ")
                      (:map (:keyword ":a") (:whitespace " ")
                            (:symbol "a"))))
  #_ '(:map
       (:keyword ":a") (:whitespace " ")
       (:symbol "a"))

  )

;; the point of this is splicing
(defn create-equals-form
  [expected-node actual-node]
  (conj (ast/first-form-vec "(= )")
        (rewrite-expected expected-node)
        (ast/first-form " ")
        actual-node))

(comment

  (create-equals-form '(:comment ";; => 2")
                      '(:list
                        (:symbol "+") (:whitespace " ")
                        (:number "1") (:whitespace " ")
                        (:number "1")))
  #_ '[:list
       [:symbol "="] [:whitespace " "]
       (:number "2") [:whitespace " "]
       (:list
        (:symbol "+") (:whitespace " ")
        (:number "1") (:whitespace " ")
        (:number "1"))]

  )

(defn prune-stack
  [stack]
  (->> (reverse stack)
       (drop-while ast/whitespace?)
       rest
       reverse))

(comment

  (prune-stack [[:whitespace " "]
                [:number "1"]
                [:keyword "a"]
                [:whitespace " "]
                [:whitespace " "]])
  ;; => [[:whitespace " "] [:number "1"]]

  )

(defn splice-after-ns-form
  [nodes more-nodes]
  (utils/splice-after-vec nodes
                          ast/ns-form?
                          more-nodes))

(comment

  (def src-with-ns
    (cs/join "\n"
             [";; fun comment"
              "(ns fun-namespace.main)"
              "(def a 1)"]))

  (filter (fn [node]
            (ast/ns-form? node))
          (ast/forms src-with-ns))
  #_ '((:list
        (:symbol "ns") (:whitespace " ")
        (:symbol "fun-namespace.main")))

  (utils/splice-after-vec (ast/forms src-with-ns)
                          ast/ns-form?
                          [(ast/first-form "\n\n")
                           (ast/first-form ";; i am a comment")])
  #_ '[(:comment ";; fun comment")
       (:whitespace "\n")
       (:list
        (:symbol "ns") (:whitespace " ")
        (:symbol "fun-namespace.main"))
       (:whitespace "\n\n")
       (:comment ";; i am a comment")
       (:whitespace "\n")
       (:list
        (:symbol "def") (:whitespace " ")
        (:symbol "a") (:whitespace " ")
        (:number "1"))]

  ;; XXX
  (splice-after-ns-form (ast/forms src-with-ns)
                        [(ast/first-form "\n\n")
                         ;; XXX: tweak?
                         #_(require-form)
                         (ast/first-form "\n")])
  #_ '[(:comment ";; fun comment") (:whitespace "\n")
       (:list
        (:symbol "ns") (:whitespace " ")
        (:symbol "fun-namespace.main"))
       (:whitespace "\n\n")
       #_(:list
        (:symbol "require") (:whitespace " ")
        (:quote
         (:symbol "clojure.test")))
       (:whitespace "\n") (:whitespace "\n")
       (:list
        (:symbol "def") (:whitespace " ")
        (:symbol "a") (:whitespace " ")
        (:number "1"))]

  )

(defn splice-after-ns-ish-form
  [nodes more-nodes]
  (utils/splice-after-vec nodes
                          (fn [node]
                            (or (ast/ns-form? node)
                                (ast/in-ns-form? node)))
                          more-nodes))

(comment

  (def src-with-in-ns
    (cs/join "\n"
             [";; fun comment"
              "(in-ns 'fun-namespace.main)"
              "(def a 1)"]))

  (filter (fn [node]
            (or (ast/ns-form? node)
                (ast/in-ns-form? node)))
          (ast/forms src-with-in-ns))
  #_ '((:list
        (:symbol "in-ns") (:whitespace " ")
        (:quote
         (:symbol "fun-namespace.main"))))

  (utils/splice-after-vec (ast/forms src-with-ns)
                          #(or (ast/ns-form? %)
                               (ast/in-ns-form? %))
                          [(ast/first-form "\n\n")
                           (ast/first-form ";; i am a comment")])
  #_ '[(:comment ";; fun comment")
       (:whitespace "\n")
       (:list
        (:symbol "ns") (:whitespace " ")
        (:symbol "fun-namespace.main"))
       (:whitespace "\n\n")
       (:comment ";; i am a comment")
       (:whitespace "\n")
       (:list
        (:symbol "def") (:whitespace " ")
        (:symbol "a") (:whitespace " ")
        (:number "1"))]

  ;; XXX
  (splice-after-ns-ish-form (ast/forms src-with-in-ns)
                            [(ast/first-form "\n\n")
                             ;; XXX: tweak?
                             #_(require-form)
                             (ast/first-form "\n")])
  #_ '[(:comment ";; fun comment") (:whitespace "\n")
       (:list
        (:symbol "in-ns") (:whitespace " ")
        (:quote
         (:symbol "fun-namespace.main")))
       (:whitespace "\n\n")
       #_(:list
        (:symbol "require") (:whitespace " ")
        (:quote
         (:symbol "clojure.test")))
       (:whitespace "\n") (:whitespace "\n")
       (:list
        (:symbol "def") (:whitespace " ")
        (:symbol "a") (:whitespace " ")
        (:number "1"))]

  )


