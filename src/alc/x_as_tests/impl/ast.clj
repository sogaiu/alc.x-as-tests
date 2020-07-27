(ns alc.x-as-tests.impl.ast
  (:require
   [clojure.string :as cs]
   [parcera.core :as pc]))

(comment

  (def src-with-comment
    "(comment :a)")

  (pc/ast src-with-comment)
  #_ '(:code
       (:list
        (:symbol "comment")
        (:whitespace " ")
        (:keyword ":a")))

  (-> (pc/ast src-with-comment)
      (nth 1)
      meta)
  #_ {:parcera.core/start {:row 1, :column 0},
      :parcera.core/end {:row 1, :column 12}}

  )

(defn forms
  [src]
  (some->> (pc/ast src)
           rest))

(comment

  (def src
    (cs/join "\n"
             [";; another form"
              "(+ 2 2)"]))

  (forms src)
  #_ '((:comment ";; another form")
       (:whitespace "\n")
       (:list
        (:symbol "+")
        (:whitespace " ")
        (:number "2")
        (:whitespace " ")
        (:number "2")))

  (forms "")
  ;; => '()

  )

(defn to-str
  [nodes]
  (->> (into [:code] nodes)
       pc/code))

(comment

  (to-str '[[:keyword ":a"]])
  ;; => ":a"

  (to-str '[[:list
             [:symbol "+"] [:whitespace " "]
             [:number "1"] [:whitespace " "]
             [:number "2"]]])
  ;; => "(+ 1 2)"

  )

(defn first-form
  [src]
  (first (forms src)))

(comment

  (def some-src
    (cs/join "\n"
             ["(def a 1)"
              ""
              ":b"]))

  (pc/ast some-src)
  #_ '(:code
       (:list
        (:symbol "def") (:whitespace " ")
        (:symbol "a") (:whitespace " ")
        (:number "1"))
       (:whitespace "\n\n")
       (:keyword ":b"))

  (first-form some-src)
  #_ '(:list
       (:symbol "def") (:whitespace " ")
       (:symbol "a") (:whitespace " ")
       (:number "1"))

  (def src-with-line-comment
    (cs/join "\n"
             [";; hi there"
              ""
              "(+ 1 1)"]))

  (first-form src-with-line-comment)
  ;; => [:comment ";; hi there"]

  )

(defn first-form-vec
  [src]
  (vec (first-form src)))

(comment

  (def some-src-again
    (cs/join "\n"
             ["(def a 1)"
              ""
              ":b"]))

  (first-form-vec some-src-again)
  #_ '[:list
       (:symbol "def") (:whitespace " ")
       (:symbol "a") (:whitespace " ")
       (:number "1")]

)

(defn has-start-meta?
  [node]
  (get (meta node) :parcera.core/start))

(comment

  (has-start-meta? (first-form "(comment :a)"))
  ;; => {:row 1, :column 0}

  (has-start-meta? '(:list
                     (:symbol "comment")))
  ;; => nil

 )

(defn start-row
  [node]
  (when-let [{:keys [row]} (has-start-meta? node)]
    row))

(comment

  (start-row (first-form "(comment :a)"))
  ;; => 1

  (start-row '(:list
               (:symbol "comment")))
  ;; => nil

 )

(defn comment-symbol?
  [ast]
  (when-let [head-elt (first ast)]
    (when (= head-elt :symbol)
      (when-let [next-elt (second ast)]
        (= next-elt "comment")))))

(comment

  (comment-symbol? '(:symbol "comment"))
  ;; => true

  )

(defn comment-block?
  [ast]
  (when-let [head-elt (first ast)]
    (when (= head-elt :list)
      (comment-symbol? (second ast)))))

(comment

  (def a-comment-block
    '(:list (:symbol "comment")))

  (comment-block? a-comment-block)
  ;; => true

  (comment-block? '(:comment ";; => 2"))
  ;; => nil

  (def src-with-comment-and-def
    (cs/join "\n"
             [""
              "(comment"
              ""
              "  (def b 2)"
              ""
              ")"
              ""
              "(def x 1)"]))

  (->> (pc/ast src-with-comment-and-def)
       rest
       (filter #(comment-block? %))
       count)
  ;; => 1

  )

(defn unwrap-comment-block
  [ast]
  (when (comment-block? ast)
    (rest (rest ast))))

(comment

  (def comment-block-ast
    '(:list
      (:symbol "comment")
      (:number "1")))

  (comment-block? comment-block-ast)
  ;; => true

  (unwrap-comment-block comment-block-ast)
  ;; => [[:number "1"]]

  (def empty-comment-block-ast
    '(:list (:symbol "comment")))

  (unwrap-comment-block empty-comment-block-ast)
  ;; => '()

  )

(defn update-forms
  [src a-fn]
  (->> (forms src)
       ;; operate on the interior nodes
       a-fn
       ;; for debugging
       ;;((fn [x] (print "after a-fn: " x) x))
       ;; wrap result back up
       to-str))

(comment

  (def src-with-comment-block-and-def
    (cs/join "\n"
             [""
              "(comment"
              ""
              "  (def y 2)"
              ""
              ")"
              ""
              "(def x 1)"
              ""]))

  (update-forms src-with-comment-block-and-def
    #(reduce (fn [acc elt]
               (if (not (comment-block? elt))
                 (conj acc elt)
                 (into acc (unwrap-comment-block elt))))
             []
             %))
  ;; => "\n\n\n  (def y 2)\n\n\n\n(def x 1)\n"

  )

(defn unwrap-comment-blocks
  [nodes]
  (reduce (fn [acc elt]
               (if (not (comment-block? elt))
                 (conj acc elt)
                 (into acc (unwrap-comment-block elt))))
          []
          nodes))

(comment

  (def src-small
    "(comment :a)")

  (update-forms src-small unwrap-comment-blocks)
  ;; => " :a"

  (def src-with-keyword-and-comment-block
    (cs/join "\n"
             [":a"
              ""
              "(comment"
              ""
              "  (def b 2)"
              ""
              "  (+ b 1)"
              "  ;; => 3"
              ""
              ")"]))

  (update-forms src-with-keyword-and-comment-block
                unwrap-comment-blocks)
  ;; => ":a\n\n\n\n  (def b 2)\n\n  (+ b 1)\n  ;; => 3\n\n"

  )

(defn line-comment?
  [ast]
  (some-> (first ast)
          (= :comment)))

(comment

  (line-comment? '(:comment ";; hi there"))
  ;; => true

  )

(defn comment-string
  [ast]
  (when (line-comment? ast)
    (second ast)))

(comment

  (comment-string '(:comment ";; smile!"))
  ;; => ";; smile!"

  )

(defn line-comment-with-expected?
  [ast]
  (and (line-comment? ast)
       (re-matches #"^;;\s*=>\s*(.*)"
                   (comment-string ast))))

(comment

  (re-matches #"^;;\s*=>\s*(.*)"
              ";; => 2")
  ;; [";; => 2" "2"]

  (line-comment-with-expected? '(:comment ";; just a normal one"))
  ;; => nil

  (line-comment-with-expected? '(:comment ";; => :expected-value"))
  ;; => [";; => :expected-value" ":expected-value"]

  )

(defn expected-from-line-comment
  [expected-comment]
  (when-let [[_ expected-form]
             (line-comment-with-expected? expected-comment)]
    (first-form expected-form)))

(comment

  (expected-from-line-comment '(:comment ";; => 2"))
  ;; => [:number "2"]

  )

(defn whitespace?
  [ast]
  (some-> (first ast)
          (= :whitespace)))

(comment

  (whitespace? '(:whitespace "\n  "))
  ;; => true

)

(defn whitespace-str
  [node]
  (when (whitespace? node)
    (second node)))

(comment

  (whitespace-str '(:whitespace " \n "))
  ;; => " \n "

  )

(defn discard-with-form?
  [ast]
  (some-> (first ast)
          (= :discard)))

(comment

  (def src-with-discard
    "#_ {:a 1}")

  (pc/ast src-with-discard)
  #_ '(:code
       (:discard
        (:whitespace " ")
        (:map
         (:keyword ":a") (:whitespace " ")
         (:number "1"))))

  (discard-with-form? (first-form src-with-discard))
  ;; => true

  (discard-with-form?
   '(:discard
     (:whitespace " ")
     (:map
      (:keyword ":a") (:whitespace " ")
      (:number "1"))))
  ;; => true

  )

;; from Clojure.g4 in parcera:
;;
;; discard: '#_' (whitespace? discard)? whitespace? form;
(defn undiscard
  [ast]
  (when (discard-with-form? ast)
    (->> (rest ast)
         (remove whitespace?)
         ;; discard can "stack"
         (remove discard-with-form?)
         first)))

(comment

  (undiscard '(:discard
               (:whitespace " ")
               (:map
                (:keyword ":a") (:whitespace " ")
                (:number "1"))))
  #_ '(:map
       (:keyword ":a") (:whitespace " ")
       (:number "1"))

  (undiscard '(:discard
               (:map
                (:keyword ":a") (:whitespace " ")
                (:number "1"))))
  #_ '(:map
       (:keyword ":a") (:whitespace " ")
       (:number "1"))

  ;; stacked discard forms parse like this:
  (first-form "#_ #_ :a :b")
  #_ '(:discard
       (:whitespace " ")
       (:discard
        (:whitespace " ")
        (:keyword ":a"))
       (:whitespace " ")
       (:keyword ":b"))

  (undiscard (first-form "#_ #_ :a :b"))
  ;; => '(:keyword ":b")

  )

(defn expected-from-discard
  [discard-with-form]
  (undiscard discard-with-form))

(comment

  (expected-from-discard
   '(:discard
     (:whitespace " ")
     (:map (:keyword ":a") (:whitespace " ")
           (:symbol "a"))))
  #_ '(:map
       (:keyword ":a")
       (:whitespace " ")
       (:symbol "a"))

  )

(defn list-node?
  [ast]
  (some-> (first ast)
          (= :list)))

(comment

  (list-node? (first-form "(+ 1 1)"))
  ;; => true

  )

;; XXX: determine what else needs to be ignored
(defn list-head
  [ast]
  (assert (list-node? ast) (str "not a list: " ast))
  (->> (rest ast)
       (drop-while (fn [node]
                     ;; XXX: other things to filter out?
                     (or (whitespace? node)
                         (line-comment? node)
                         (comment-block? node)
                         (discard-with-form? node))))
       first))

(comment

  (first-form "(+ 1 1)")
  #_ '(:list
       (:symbol "+") (:whitespace " ")
       (:number "1") (:whitespace " ")
       (:number "1"))

  (list-head (first-form "(+ 1 1)"))
  ;; => '(:symbol "+")

  (list-head (first-form "( + 1 1)"))
  ;; => '(:symbol "+")

  (list-head (first-form (cs/join "\n"
                                  ["(;; hi"
                                   "+ 1 1)"])))
  ;; => '(:symbol "+")

  (list-head (first-form (cs/join "\n"
                                  ["((comment :a)\n"
                                   "+ 1 1)"])))
  ;; => '(:symbol "+")

  (list-head (first-form "(#_ - + 1 1)"))
  ;; => '(:symbol "+")

  )

(defn symbol-node?
  [ast]
  (some-> (first ast)
          (= :symbol)))

(comment

  (symbol-node? (first-form "hi"))
  ;; => true

  (symbol-node? (first-form ":hi"))
  ;; => false

  )

(defn symbol-name
  [ast]
  (assert (symbol-node? ast) (str "not symbol node: " ast))
  (second ast))

(comment

  (symbol-name (first-form "hi"))
  ;; => "hi"

  )

(defn ns-form?
  [ast]
  (and (list-node? ast)
       (symbol-node? (list-head ast))
       (= "ns" (symbol-name (list-head ast)))))

(comment

  (def src-with-ns
    "(ns fun-namespace.main)")

  (first-form src-with-ns)
  #_ '(:list
        (:symbol "ns") (:whitespace " ")
        (:symbol "fun-namespace.main"))

  (ns-form? (first-form src-with-ns))
  ;; => true

  )

(defn in-ns-form?
  [ast]
  (and (list-node? ast)
       (symbol-node? (list-head ast))
       (= "in-ns" (symbol-name (list-head ast)))))

(comment

  (def src-with-in-ns
    "(in-ns 'clojure.core)")

  (first-form src-with-in-ns)
  #_ '(:list
       (:symbol "in-ns") (:whitespace " ")
       (:quote
        (:symbol "clojure.core")))

  (in-ns-form? (first-form src-with-in-ns))

  )

(defn has-ns-form?
  [nodes]
  (some ns-form? nodes))

(comment

  (def src-with-ns-and-other
    (cs/join "\n"
             [";; fun comment"
              "(ns fun-namespace.main)"
              "(def a 1)"]))

  (has-ns-form? (forms src-with-ns-and-other))
  ;; => true

  (def src-without-ns-form
    (cs/join "\n"
             [";; fun comment"
              "(in-ns 'fun-namespace.main)"
              "(def a 1)"]))

  (has-ns-form? (forms src-without-ns-form))
  ;; => nil

  )

(defn has-ns-ish-form?
  [nodes]
  (some #(or (ns-form? %)
             (in-ns-form? %))
        nodes))

(comment

  (def src-with-ns-and-other-again
    (cs/join "\n"
             [";; fun comment"
              "(ns fun-namespace.main)"
              "(def a 1)"]))

  (has-ns-ish-form? (forms src-with-ns-and-other-again))
  ;; => true

  (def src-without-ns-form-again
    (cs/join "\n"
             [";; fun comment"
              "(in-ns 'fun-namespace.main)"
              "(def a 1)"]))

  (has-ns-ish-form? (forms src-without-ns-form-again))
  ;; => true

  )

(defn collapse-whitespace
  [ws-str]
  (cond
    (cs/includes? ws-str "\n\n")
    "\n\n"
    ;;
    (cs/includes? ws-str "\n")
    "\n"
    ;;
    :else
    " "))

(comment

  (collapse-whitespace "  ")
  ;; => " "

  (collapse-whitespace "\n\n")
  ;; => "\n\n"

  (collapse-whitespace "\n ")
  ;; => "\n"

  (collapse-whitespace " \n")
  ;; => "\n"

  )

(defn merge-whitespace
  [nodes]
  (let [[merged ws-node]
        (reduce (fn [[merged ws-node] node]
                  ;; immediately previous node was whitespace node
                  (if ws-node
                    (let [prev-ws (whitespace-str ws-node)]
                      (if (whitespace? node)
                        (let [ws (whitespace-str node)]
                          [merged [:whitespace
                                   (collapse-whitespace (str prev-ws ws))]])
                        [(conj merged ws-node node) nil]))
                    ;; immediately previous node wasn't whitespace node
                    (if (whitespace? node)
                      (let [ws (whitespace-str node)]
                        [merged [:whitespace (collapse-whitespace ws)]])
                      [(conj merged node) nil])))
                [[] nil]
                nodes)]
    ;; handles possibility of non-nil ws-node
    (if ws-node
      (conj merged ws-node)
      merged)))

(comment

  (merge-whitespace '((:whitespace " ")
                      (:whitespace " ")))
  #_ '((:whitespace " "))

  (merge-whitespace '((:whitespace "\n")
                      (:whitespace "\n")))
  #_ '((:whitespace "\n\n"))

  (merge-whitespace '((:whitespace "\n")
                      (:whitespace " ")))
  #_ '((:whitespace "\n"))

  (merge-whitespace '((:whitespace " ")
                      (:whitespace "\n")))
  #_ '((:whitespace "\n"))

  (merge-whitespace '((:keyword ":a")
                      (:whitespace " ")
                      (:whitespace "\n")))
  #_ '[(:keyword ":a")
       [:whitespace "\n"]]

  (merge-whitespace '((:keyword ":a")
                      (:whitespace "\n")
                      (:whitespace " ")
                      (:whitespace "\n")
                      (:keyword ":b")))
  #_ '[(:keyword ":a")
       [:whitespace "\n\n"]
       (:keyword ":b")]

  )

(defn update-forms-and-format
  [src a-fn]
  (->> (forms src)
       ;; operate on the interior nodes
       a-fn
       ;; for debugging
       ;;((fn [x] (print "after a-fn: " x) x))
       merge-whitespace
       ;; wrap up and convert to string
       to-str))

(comment

  (update-forms-and-format
   (str (cs/join "\n"
                 ["(comment"
                  ""
                  "  (def a 1)"
                  ""
                  "  (+ a 1)"
                  "  ;; => 2"
                  ""
                  "  (def b 1)"
                  ""
                  "  (conj {:a a} [:b b])"
                  "  #_ {:a a"
                  "      :b b}"
                  ""
                  ")"]))
   unwrap-comment-blocks)
  #_ "

(def a 1)

(+ a 1)
;; => 2

(def b 1)

(conj {:a a} [:b b])
#_ {:a a
      :b b}

"

  )
