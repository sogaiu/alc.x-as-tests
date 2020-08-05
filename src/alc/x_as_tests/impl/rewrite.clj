(ns alc.x-as-tests.impl.rewrite
  (:require
   [alc.x-as-tests.impl.ast :as ast]
   [alc.x-as-tests.impl.utils :as utils]
   [clojure.string :as cs]))

(defn last-non-whitespace
  [nodes]
  (utils/last-not nodes ast/whitespace?))

(comment

  (last-non-whitespace [(ast/first-form " ")
                        (ast/first-form "1")])
  ;; => (ast/first-form "1")

  (last-non-whitespace [(ast/first-form ":b")
                        (ast/first-form "\n")])
  ;; => (ast/first-form ":b")

  (last-non-whitespace [])
  ;; => nil

  )

(defn create-deftest-opening
  [test-name]
  (ast/first-form-vec
   (str "(clojure.test/deftest " test-name ")")))

(comment

  (ast/to-str
   [(create-deftest-opening "test-at-line-7650")])
  ;; => "(clojure.test/deftest test-at-line-7650)"

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

  (ast/to-str
   [(rewrite-expected (ast/first-form ";; => 2"))])
  ;; => "2"

  (ast/to-str
   [(rewrite-expected (ast/first-form "#_ {:a a}"))])
  ;; => "{:a a}"

  )

;; the point of this is splicing
(defn create-equals-form
  [expected-node actual-node]
  (conj (ast/first-form-vec "(= )")
        (rewrite-expected expected-node)
        (ast/first-form " ")
        actual-node))

(comment

  (ast/to-str
   [(create-equals-form (ast/first-form ";; => 2")
                        (ast/first-form "(+ 1 1)"))])
  ;; => "(= 2 (+ 1 1))"

  )

(defn create-is-form
  [actual-node expected-node]
  (conj (ast/first-form-vec "(clojure.test/is )")
        (create-equals-form expected-node actual-node)))

(comment

  (ast/to-str
   [(create-is-form (ast/first-form "(+ 1 1)")
                    (ast/first-form ";; => 2"))])
  ;; => "(clojure.test/is (= 2 (+ 1 1)))"

 )

(defn ensure-leading-newline
  [stack]
  (let [first-node (first stack)]
    (if (ast/has-newline? first-node)
      (vec stack)
      (into [(ast/first-form "\n")] stack))))

(comment

  (ensure-leading-newline [(ast/first-form " ")
                           (ast/first-form "1")])
  #_ [(ast/first-form "\n")
      (ast/first-form " ")
      (ast/first-form "1")]

  (ensure-leading-newline [(ast/first-form "1")])
  #_ [(ast/first-form "\n")
      (ast/first-form "1")]

  )

;; XXX: deftest style for the moment
(defn rewrite-as-test
  [actual-node expected-node stack]
  (assert (ast/has-start-meta? actual-node)
          "actual node missing location info")
  (let [test-name (str "test-at-line-"
                       (ast/start-row actual-node))]
    ;; splicing seems cumbersome
    (into (create-deftest-opening test-name)
          (conj (ensure-leading-newline stack)
                (create-is-form actual-node expected-node)))))

(comment

  (ast/to-str
   [(ast/first-form
     (str "(clojure.test/deftest name-of-test\n"
          "  (clojure.test/is (= 4 (+ 2 2))))"))])
  #_
"(clojure.test/deftest name-of-test
  (clojure.test/is (= 4 (+ 2 2))))"

  (def src-with-comment-block-test
    (cs/join "\n"
             ["(comment"
              "  (+ 1 1)"
              "  ;; => 2"
              ")"]))

  (ast/to-str
   [(let [[actual expected]
          (->> (ast/unwrap-comment-block
                (ast/first-form src-with-comment-block-test))
               (filter (fn [node]
                         (not (ast/whitespace? node)))))]
      (rewrite-as-test actual expected []))])
  #_
"(clojure.test/deftest test-at-line-2
(clojure.test/is (= 2 (+ 1 1))))"

  (ast/to-str
   [(rewrite-as-test (ast/first-form "(+ 1 1)")
                     (ast/first-form ";; => 2")
                     [])])
  #_
"(clojure.test/deftest test-at-line-1
(clojure.test/is (= 2 (+ 1 1))))"

  (ast/to-str
   [(rewrite-as-test (ast/first-form "(+ 1 1)")
                     (ast/first-form ";; => 2")
                     [(ast/first-form "(def b 1)")
                      (ast/first-form "\n\n  ")])])
  #_
"(clojure.test/deftest test-at-line-1
(def b 1)

  (clojure.test/is (= 2 (+ 1 1))))"

  )

(defn prune-stack
  [stack]
  (->> (reverse stack)
       (drop-while ast/whitespace?)
       rest
       reverse))

(comment

  (prune-stack [(ast/first-form " ")
                (ast/first-form "1")
                (ast/first-form "a")
                (ast/first-form " ")
                (ast/first-form " ")])
  #_ [(ast/first-form " ")
      (ast/first-form "1")]

  )

(defn rewrite-comment-block
  [comment-block]
  (some->>
   (ast/unwrap-comment-block comment-block)
   (reduce (fn [[rewritten stack] elt]
             (let [last-non-ws-form (last-non-whitespace stack)]
               (if (not (and last-non-ws-form
                             ;; XXX: abstract out?
                             (or (ast/discard-with-form? elt)
                                 (ast/line-comment-with-expected? elt))))
                 ;; pass through
                 [rewritten (conj stack elt)]
                 ;; rewrite
                 [(conj rewritten
                        ;; XXX: tweaking this leads to test tweaking...
                        (ast/first-form "\n\n")
                        (rewrite-as-test last-non-ws-form
                                         elt
                                         (prune-stack stack)))
                  []])))
           ;; rewritten and stack
           [[] []])
   first))

(comment

  (ast/to-str
   (rewrite-comment-block
    (ast/first-form (str "(comment\n"
                         "  (+ 1 1)\n"
                         "  ;; => 2\n"
                         ")"))))
  #_ "

(clojure.test/deftest test-at-line-2
  (clojure.test/is (= 2 (+ 1 1))))"

  (ast/to-str
   (rewrite-comment-block
    (ast/first-form
     (cs/join "\n"
              ["(comment"
               "  (def a 1)"
               ""
               "  (+ a 1)"
               "  ;; => 2"
               ""
               "  (def b 1)"
               ""
               "  (+ a b)"
               "  ;; => 3"
               ")"]))))
  #_ "

(clojure.test/deftest test-at-line-4
  (def a 1)

  (clojure.test/is (= 2 (+ a 1))))

(clojure.test/deftest test-at-line-9

  (def b 1)

  (clojure.test/is (= 3 (+ a b))))"

  )

(defn rewrite-comment-blocks-with-tests
  [nodes]
  (reduce (fn [acc elt]
            (if (not (ast/comment-block? elt))
              (conj acc elt)
              (into acc (rewrite-comment-block elt))))
          []
          nodes))

(comment

  (def src-with-one-comment-block-with-one-test
    (cs/join "\n"
             ["(comment"
              "  (+ 1 1)"
              "  ;; => 2"
              ")"]))

  (ast/update-forms-and-format src-with-one-comment-block-with-one-test
                               rewrite-comment-blocks-with-tests)
  #_ "

(clojure.test/deftest test-at-line-2
  (clojure.test/is (= 2 (+ 1 1))))"

  (def src-with-one-comment-block-with-two-tests
    (cs/join "\n"
             [":x"
              ""
              "(comment"
              ""
              "  (def a 1)"
              ""
              "  (+ a 1)"
              "  ;; => 2"
              ""
              "  (def b 1)"
              ""
              "  (+ a b)"
              "  ;; => 3"
              ""
              ")"
              ""
              ":y"]))

  (ast/update-forms-and-format src-with-one-comment-block-with-two-tests
                               rewrite-comment-blocks-with-tests)
  #_ ":x

(clojure.test/deftest test-at-line-7

  (def a 1)

  (clojure.test/is (= 2 (+ a 1))))

(clojure.test/deftest test-at-line-12

  (def b 1)

  (clojure.test/is (= 3 (+ a b))))

:y"

  (def src-with-two-comment-blocks
    (cs/join "\n"
             [":a"
              ""
              "(comment"
              ""
              "  (+ 1 1)"
              "  ;; => 2"
              ")"
              ""
              ":c"
              ""
              "(comment"
              ""
              "  (- 21 1)"
              "  ;; => 20"
              ")"]))

  (ast/update-forms-and-format src-with-two-comment-blocks
                               rewrite-comment-blocks-with-tests)
  #_ ":a

(clojure.test/deftest test-at-line-5

  (clojure.test/is (= 2 (+ 1 1))))

:c

(clojure.test/deftest test-at-line-13

  (clojure.test/is (= 20 (- 21 1))))"

  (def line-comment-and-discard-tests
    (cs/join "\n"
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

  (ast/update-forms-and-format line-comment-and-discard-tests
                               rewrite-comment-blocks-with-tests)
  #_ "

(clojure.test/deftest test-at-line-5

  (def a 1)

  (clojure.test/is (= 2 (+ a 1))))

(clojure.test/deftest test-at-line-10

  (def b 1)

  (clojure.test/is (= {:a a
      :b b} (conj {:a a} [:b b]))))"

  )

;; XXX: this has "test-at-line-" baked in
(defn remove-existing-tests-form
  []
  (ast/first-form
   ;; XXX: not so great for maintenance?
   (cs/join "\n"
            ["(->> (keys (ns-interns *ns*))"
             "     (filter (fn [test-sym]"
             "       (re-matches #\"^test-at-line-.*\""
             "         (name test-sym))))"
             "     (run! (fn [test-sym]"
             "             (ns-unmap *ns* test-sym))))"])))

(comment

  (re-matches #"^test-at-line-.*"
              "test-at-line-8")
  ;; => "test-at-line-8"

  (re-matches #"^test-at-line-.*"
              "test-not-at-line-8")
  ;; => nil

  (ast/to-str [(remove-existing-tests-form)])
  #_
"(->> (keys (ns-interns *ns*))
     (filter (fn [test-sym]
       (re-matches #\"^test-at-line-.*\"
         (name test-sym))))
     (run! (fn [test-sym]
             (ns-unmap *ns* test-sym))))"

  )

(defn require-form
  []
  (ast/first-form "(require 'clojure.test)"))

(comment

  (ast/to-str [(require-form)])
  ;; => "(require 'clojure.test)"

  )

(defn run-tests-with-summary-form
  []
  (ast/first-form
   (cs/join "\n" ["(binding [clojure.test/*report-counters*"
                  "          (ref clojure.test/*initial-report-counters*)]"
                  "  (clojure.test/do-report"
                  "    (clojure.test/test-vars"
                  "      (->> (keys (ns-interns *ns*))"
                  "           (keep"
                  "             (fn [test-sym]"
                  "               (let [[_ test-num]"
                  "                     (re-matches #\"^test-at-line-(.*)\""
                  "                                 (name test-sym))]"
                  "                 (when test-num"
                  "                   [(Integer. test-num)"
                  "                    (intern *ns* test-sym)]))))"
                  "           (sort-by"
                  "             (fn [[num test-var]]"
                  "               num))"
                  "           (map second))))"
                  "  @clojure.test/*report-counters*)"])))

(comment

  (ast/to-str
   [(run-tests-with-summary-form)])
  #_
"(binding [clojure.test/*report-counters*
          (ref clojure.test/*initial-report-counters*)]
  (clojure.test/do-report
    (clojure.test/test-vars
      (->> (keys (ns-interns *ns*))
           (keep
             (fn [test-sym]
               (let [[_ test-num]
                     (re-matches #\"^test-at-line-(.*)\"
                                 (name test-sym))]
                 (when test-num
                   [(Integer. test-num)
                    (intern *ns* test-sym)]))))
           (sort-by
             (fn [[num test-var]]
               num))
           (map second))))
  @clojure.test/*report-counters*)"

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

  (ast/to-str
   (filter (fn [node]
             (ast/ns-form? node))
           (ast/forms src-with-ns)))
  ;; => "(ns fun-namespace.main)"

  (ast/to-str
   (utils/splice-after-vec (ast/forms src-with-ns)
                           ast/ns-form?
                           [(ast/first-form "\n\n")
                            (ast/first-form ";; i am a comment")]))
  #_
";; fun comment
(ns fun-namespace.main)

;; i am a comment
(def a 1)"

  (ast/to-str
   (splice-after-ns-form (ast/forms src-with-ns)
                         [(ast/first-form "\n\n")
                          (require-form)
                          (ast/first-form "\n")]))
  #_
";; fun comment
(ns fun-namespace.main)

(require 'clojure.test)

(def a 1)"

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

  (ast/to-str
   (filter (fn [node]
             (or (ast/ns-form? node)
                 (ast/in-ns-form? node)))
           (ast/forms src-with-in-ns)))
  ;; => "(in-ns 'fun-namespace.main)"

  (ast/to-str
   (utils/splice-after-vec (ast/forms src-with-ns)
                           #(or (ast/ns-form? %)
                                (ast/in-ns-form? %))
                           [(ast/first-form "\n\n")
                            (ast/first-form ";; i am a comment")]))
  #_
";; fun comment
(ns fun-namespace.main)

;; i am a comment
(def a 1)"

  (ast/to-str
   (splice-after-ns-ish-form (ast/forms src-with-in-ns)
                             [(ast/first-form "\n\n")
                              (require-form)
                              (ast/first-form "\n")]))
  #_
";; fun comment
(in-ns 'fun-namespace.main)

(require 'clojure.test)

(def a 1)"

  )

(defn rewrite-with-tests
  [src]
  (let [nls (ast/first-form "\n\n")
        test-prep-forms [nls
                         (remove-existing-tests-form)
                         nls
                         (require-form)]
        test-summary-forms [nls
                            (run-tests-with-summary-form)
                            nls]]
    (ast/update-forms-and-format
     src
     (fn [nodes]
       (let [without-prep-forms
             (into (rewrite-comment-blocks-with-tests nodes)
                   test-summary-forms)]
         (if (ast/has-ns-ish-form? nodes)
           (splice-after-ns-ish-form without-prep-forms
                                     test-prep-forms)
           ;; no ns or in-ns form
           (into test-prep-forms
                 without-prep-forms)))))))

(comment

  (rewrite-with-tests
   (cs/join "\n"
            ["(ns my.ns)"
             "(comment"
             ""
             "  (+ 1 7)"
             "  ;; => 8"
             ""
             "  (conj [:a] :b)"
             "  #_ [:a"
             "      :b]"
             ""
             ")"]))
  #_ "(ns my.ns)

(->> (keys (ns-interns *ns*))
     (filter (fn [test-sym]
       (re-matches #\"^test-at-line-.*\"
         (name test-sym))))
     (run! (fn [test-sym]
             (ns-unmap *ns* test-sym))))

(require 'clojure.test)

(clojure.test/deftest test-at-line-4

  (clojure.test/is (= 8 (+ 1 7))))

(clojure.test/deftest test-at-line-7

  (clojure.test/is (= [:a
      :b] (conj [:a] :b))))

(binding [clojure.test/*report-counters*
          (ref clojure.test/*initial-report-counters*)]
  (clojure.test/do-report
    (clojure.test/test-vars
      (->> (keys (ns-interns *ns*))
           (keep
             (fn [test-sym]
               (let [[_ test-num]
                     (re-matches #\"^test-at-line-(.*)\"
                                 (name test-sym))]
                 (when test-num
                   [(Integer. test-num)
                    (intern *ns* test-sym)]))))
           (sort-by
             (fn [[num test-var]]
               num))
           (map second))))
  @clojure.test/*report-counters*)

"

  (rewrite-with-tests
   (cs/join "\n"
            ["(in-ns 'my.ns)"
             ""
             "(comment"
             ""
             "  (+ 1 7)"
             "  ;; => 8"
             ""
             "  (conj [:a] :b)"
             "  #_ [:a"
             "      :b]"
             ""
             ")"]))
  #_ "(in-ns 'my.ns)

(->> (keys (ns-interns *ns*))
     (filter (fn [test-sym]
       (re-matches #\"^test-at-line-.*\"
         (name test-sym))))
     (run! (fn [test-sym]
             (ns-unmap *ns* test-sym))))

(require 'clojure.test)

(clojure.test/deftest test-at-line-5

  (clojure.test/is (= 8 (+ 1 7))))

(clojure.test/deftest test-at-line-8

  (clojure.test/is (= [:a
      :b] (conj [:a] :b))))

(binding [clojure.test/*report-counters*
          (ref clojure.test/*initial-report-counters*)]
  (clojure.test/do-report
    (clojure.test/test-vars
      (->> (keys (ns-interns *ns*))
           (keep
             (fn [test-sym]
               (let [[_ test-num]
                     (re-matches #\"^test-at-line-(.*)\"
                                 (name test-sym))]
                 (when test-num
                   [(Integer. test-num)
                    (intern *ns* test-sym)]))))
           (sort-by
             (fn [[num test-var]]
               num))
           (map second))))
  @clojure.test/*report-counters*)

"

  (rewrite-with-tests
   (cs/join "\n"
            ["(comment"
             ""
             "  (+ 1 7)"
             "  ;; => 8"
             ""
             "  (conj [:a] :b)"
             "  #_ [:a"
             "      :b]"
             ""
             ")"]))
  #_ "

(->> (keys (ns-interns *ns*))
     (filter (fn [test-sym]
       (re-matches #\"^test-at-line-.*\"
         (name test-sym))))
     (run! (fn [test-sym]
             (ns-unmap *ns* test-sym))))

(require 'clojure.test)

(clojure.test/deftest test-at-line-3

  (clojure.test/is (= 8 (+ 1 7))))

(clojure.test/deftest test-at-line-6

  (clojure.test/is (= [:a
      :b] (conj [:a] :b))))

(binding [clojure.test/*report-counters*
          (ref clojure.test/*initial-report-counters*)]
  (clojure.test/do-report
    (clojure.test/test-vars
      (->> (keys (ns-interns *ns*))
           (keep
             (fn [test-sym]
               (let [[_ test-num]
                     (re-matches #\"^test-at-line-(.*)\"
                                 (name test-sym))]
                 (when test-num
                   [(Integer. test-num)
                    (intern *ns* test-sym)]))))
           (sort-by
             (fn [[num test-var]]
               num))
           (map second))))
  @clojure.test/*report-counters*)

"

  )

(defn rewrite-without-non-comment-blocks
  [src]
  (let [nls (ast/first-form "\n\n")
        test-prep-forms [nls
                         (remove-existing-tests-form)
                         nls
                         (require-form)]
        test-summary-forms [nls
                            (run-tests-with-summary-form)
                            nls]]
    (ast/update-forms-and-format
     src
     (fn [nodes]
       (into test-prep-forms
             (into (rewrite-comment-blocks-with-tests
                    (filter ast/comment-block? nodes))
                   test-summary-forms))))))

(comment

  (rewrite-without-non-comment-blocks
   (cs/join "\n"
            ["(comment"
             ""
             "  (+ 1 7)"
             "  ;; => 8"
             ""
             "  (conj [:a] :b)"
             "  #_ [:a"
             "      :b]"
             ""
             ")"]))
  #_ "

(->> (keys (ns-interns *ns*))
     (filter (fn [test-sym]
       (re-matches #\"^test-at-line-.*\"
         (name test-sym))))
     (run! (fn [test-sym]
             (ns-unmap *ns* test-sym))))

(require 'clojure.test)

(clojure.test/deftest test-at-line-3

  (clojure.test/is (= 8 (+ 1 7))))

(clojure.test/deftest test-at-line-6

  (clojure.test/is (= [:a
      :b] (conj [:a] :b))))

(binding [clojure.test/*report-counters*
          (ref clojure.test/*initial-report-counters*)]
  (clojure.test/do-report
    (clojure.test/test-vars
      (->> (keys (ns-interns *ns*))
           (keep
             (fn [test-sym]
               (let [[_ test-num]
                     (re-matches #\"^test-at-line-(.*)\"
                                 (name test-sym))]
                 (when test-num
                   [(Integer. test-num)
                    (intern *ns* test-sym)]))))
           (sort-by
             (fn [[num test-var]]
               num))
           (map second))))
  @clojure.test/*report-counters*)

"

  (rewrite-without-non-comment-blocks
   (cs/join "\n"
            ["(ns my.ns)"
             ""
             "(comment"
             ""
             "  (+ 1 7)"
             "  ;; => 8"
             ""
             "  (conj [:a] :b)"
             "  #_ [:a"
             "      :b]"
             ""
             "(def fun :a)"
             ")"]))
  #_ "

(->> (keys (ns-interns *ns*))
     (filter (fn [test-sym]
       (re-matches #\"^test-at-line-.*\"
         (name test-sym))))
     (run! (fn [test-sym]
             (ns-unmap *ns* test-sym))))

(require 'clojure.test)

(clojure.test/deftest test-at-line-5

  (clojure.test/is (= 8 (+ 1 7))))

(clojure.test/deftest test-at-line-8

  (clojure.test/is (= [:a
      :b] (conj [:a] :b))))

(binding [clojure.test/*report-counters*
          (ref clojure.test/*initial-report-counters*)]
  (clojure.test/do-report
    (clojure.test/test-vars
      (->> (keys (ns-interns *ns*))
           (keep
             (fn [test-sym]
               (let [[_ test-num]
                     (re-matches #\"^test-at-line-(.*)\"
                                 (name test-sym))]
                 (when test-num
                   [(Integer. test-num)
                    (intern *ns* test-sym)]))))
           (sort-by
             (fn [[num test-var]]
               num))
           (map second))))
  @clojure.test/*report-counters*)

"

  )

;; XXX: this is experimental
(defn run-test-via-path
  [path]
  (eval (read-string (str "(do"
                          (rewrite-with-tests (slurp path))
                          ")"))))

(comment

  ;; don't want this running automatically
  (comment

    (require '[alc.x-as-tests.impl.paths :as paths])

    (def a-path
      (paths/as-abspath (System/getProperty "user.dir")
                        "src" "alc" "x_as_tests" "impl"
                        "ast.clj"))

    (require 'clojure.test)

    (binding [clojure.test/*test-out* *out*]
      (let [original-ns (.name *ns*)]
        (run-test-via-path a-path)
        (in-ns original-ns)))

    )

  )
