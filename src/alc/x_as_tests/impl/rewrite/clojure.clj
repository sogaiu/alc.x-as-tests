(ns alc.x-as-tests.impl.rewrite.clojure
  (:require
   [alc.x-as-tests.impl.ast :as ast]
   [alc.x-as-tests.impl.rewrite.core :as rewrite]
   [clojure.string :as cs]))

(defn create-deftest-opening
  [test-name]
  (ast/first-form-vec
   (str "(clojure.test/deftest " test-name ")")))

(comment

  (create-deftest-opening "test-at-line-7650")
  #_ '[:list
       (:symbol "clojure.test/deftest") (:whitespace " ")
       (:symbol "test-at-line-7650")]

  )

(defn create-is-form
  [actual-node expected-node]
  (conj (ast/first-form-vec "(clojure.test/is )")
        (rewrite/create-equals-form expected-node actual-node)))

(comment

  (create-is-form '(:list
                     (:symbol "+") (:whitespace " ")
                     (:number "1") (:whitespace " ")
                     (:number "1"))
                   '(:comment ";; => 2"))
  #_ '[:list
       [:symbol "clojure.test/is"] [:whitespace " "]
       [:list
        [:symbol "="] [:whitespace " "]
        (:number "2") [:whitespace " "]
        (:list
         (:symbol "+") (:whitespace " ")
         (:number "1") (:whitespace " ")
         (:number "1"))]]

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
          (conj (vec stack)
                (create-is-form actual-node expected-node)))))

(comment

  (ast/first-form
   (str "(clojure.test/deftest name-of-test\n"
        "  (clojure.test/is (= 4 (+ 2 2))))"))
  #_ '(:list
       (:symbol "clojure.test/deftest") (:whitespace " ")
       (:symbol "name-of-test") (:whitespace "\n  ")
       (:list
        (:symbol "clojure.test/is") (:whitespace " ")
        (:list
         (:symbol "=") (:whitespace " ")
         (:number "4") (:whitespace " ")
         (:list
          (:symbol "+") (:whitespace " ")
          (:number "2") (:whitespace " ")
          (:number "2")))))

  (def src-with-comment-block-test
    (cs/join "\n"
             ["(comment"
              "  (+ 1 1)"
              "  ;; => 2"
              ")"]))

  (let [[actual expected]
        (->> (ast/unwrap-comment-block
              (ast/first-form src-with-comment-block-test))
             (filter (fn [node]
                       (not (ast/whitespace? node)))))]
    (rewrite-as-test actual expected []))
  #_ '[:list
       [:symbol "clojure.test/deftest"] [:whitespace " "]
       [:symbol "test-at-line-2"]
       [:list
        [:symbol "clojure.test/is"] [:whitespace " "]
        [:list
         [:symbol "="] [:whitespace " "]
         (:number "2") [:whitespace " "]
         (:list
          (:symbol "+") (:whitespace " ")
          (:number "1") (:whitespace " ")
          (:number "1"))]]]

  (rewrite-as-test '^{:parcera.core/start {:row 1, :column 0},
                      :parcera.core/end {:row 1, :column 12}}
                   (:list
                      (:symbol "+") (:whitespace " ")
                      (:number "1") (:whitespace " ")
                      (:number "1"))
                    '(:comment ";; => 2")
                    [])
  #_ '[:list
       [:symbol "clojure.test/deftest"] [:whitespace " "]
       [:symbol "test-at-line-1"]
       [:list
        [:symbol "clojure.test/is"] [:whitespace " "]
        [:list
         [:symbol "="] [:whitespace " "]
         (:number "2") [:whitespace " "]
         (:list
          (:symbol "+") (:whitespace " ")
          (:number "1") (:whitespace " ")
          (:number "1"))]]]

  (rewrite-as-test '^{:parcera.core/start {:row 1, :column 0},
                      :parcera.core/end {:row 1, :column 12}}
                     (:list
                      (:symbol "+") (:whitespace " ")
                      (:number "1") (:whitespace " ")
                      (:number "1"))
                    '(:comment ";; => 2")
                    ['(:list
                       (:symbol "def") (:whitespace " ")
                       (:symbol "b") (:whitespace " ")
                       (:number "1"))
                     '(:whitespace "\n\n  ")])
  #_ '[:list
       [:symbol "clojure.test/deftest"] [:whitespace " "]
       [:symbol "test-at-line-1"]
       (:list
        (:symbol "def") (:whitespace " ")
        (:symbol "b") (:whitespace " ")
        (:number "1"))
       (:whitespace "\n\n  ")
       [:list
        [:symbol "clojure.test/is"] [:whitespace " "]
        [:list
         [:symbol "="] [:whitespace " "]
         (:number "2") [:whitespace " "]
         (:list
          (:symbol "+") (:whitespace " ")
          (:number "1") (:whitespace " ")
          (:number "1"))]]]

  )

(defn rewrite-comment-block
  [comment-block]
  (some->>
   (ast/unwrap-comment-block comment-block)
   (reduce (fn [[rewritten stack] elt]
             (let [last-non-ws-form (rewrite/last-non-whitespace stack)]
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
                                         (rewrite/prune-stack stack)))
                  []])))
           ;; rewritten and stack
           [[] []])
   first))

(comment

  (rewrite-comment-block
   (ast/first-form (str "(comment\n"
                        "  (+ 1 1)\n"
                        "  ;; => 2\n"
                        ")")))
  #_ '[[:whitespace "\n\n"]
       [:list
        [:symbol "clojure.test/deftest"] [:whitespace " "]
        [:symbol "test-at-line-2"]
        (:whitespace "\n  ")
        [:list
         [:symbol "clojure.test/is"] [:whitespace " "]
         [:list
          [:symbol "="] [:whitespace " "]
          (:number "2") [:whitespace " "]
          (:list
           (:symbol "+") (:whitespace " ")
           (:number "1") (:whitespace " ")
           (:number "1"))]]]]

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
              ")"])))
  #_ '[(:whitespace "\n\n")
       [:list
        (:symbol "clojure.test/deftest") (:whitespace " ")
        (:symbol "test-at-line-4")
        (:whitespace "\n  ")
        (:list
         (:symbol "def") (:whitespace " ")
         (:symbol "a") (:whitespace " ")
         (:number "1"))
        (:whitespace "\n\n  ")
        [:list
         (:symbol "clojure.test/is") (:whitespace " ")
         [:list
          (:symbol "=") (:whitespace " ")
          (:number "2") (:whitespace " ")
          (:list
           (:symbol "+") (:whitespace " ")
           (:symbol "a") (:whitespace " ")
           (:number "1"))]]]
       (:whitespace "\n\n")
       [:list
        (:symbol "clojure.test/deftest") (:whitespace " ")
        (:symbol "test-at-line-9")
        (:whitespace "\n\n  ")
        (:list
         (:symbol "def") (:whitespace " ")
         (:symbol "b") (:whitespace " ")
         (:number "1"))
        (:whitespace "\n\n  ")
        [:list
         (:symbol "clojure.test/is") (:whitespace " ")
         [:list
          (:symbol "=") (:whitespace " ")
          (:number "3") (:whitespace " ")
          (:list
           (:symbol "+") (:whitespace " ")
           (:symbol "a") (:whitespace " ")
           (:symbol "b"))]]]]

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

  (ast/forms src-with-one-comment-block-with-one-test)
  #_ '((:list
        (:symbol "comment")
        (:whitespace "\n  ")
        (:list
         (:symbol "+") (:whitespace " ")
         (:number "1") (:whitespace " ")
         (:number "1"))
        (:whitespace "\n  ")
        (:comment ";; => 2")
        (:whitespace "\n")))

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
             "     (filter (fn [test-name]"
             "       (re-matches #\"^test-at-line-.*\""
             "         (name test-name))))"
             "     (run! (fn [test-name]"
             "             (ns-unmap *ns* test-name))))"])))

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
     (filter (fn [test-name]
       (re-matches #\"^test-at-line-.*\"
         (name test-name))))
     (run! (fn [test-name]
             (ns-unmap *ns* test-name))))"

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
                  "      (keep"
                  "        (fn [test-name]"
                  "          (when (re-matches #\"^test-at-line-.*\""
                  "                  (name test-name))"
                  "            (intern *ns* (symbol test-name))))"
                  "        (sort (keys (ns-interns *ns*))))))"
                  "  @clojure.test/*report-counters*)"])))

(comment

  (run-tests-with-summary-form)
  #_ '(:list
       (:symbol "binding") (:whitespace " ")
       (:vector
        (:symbol "clojure.test/*report-counters*")
        (:whitespace "\n          ")
        (:list
         (:symbol "ref") (:whitespace " ")
         (:symbol "clojure.test/*initial-report-counters*")))
       (:whitespace "\n  ")
       (:list
        (:symbol "clojure.test/do-report") (:whitespace "\n    ")
        (:list
         (:symbol "clojure.test/test-vars") (:whitespace "\n      ")
         (:list
          (:symbol "keep") (:whitespace "\n        ")
          (:list
           (:symbol "fn") (:whitespace " ")
           (:vector
            (:symbol "test-name")) (:whitespace "\n          ")
           (:list
            (:symbol "when") (:whitespace " ")
            (:list
             (:symbol "re-matches") (:whitespace " ")
             (:regex "\"^test-at-line-.*\"")
             (:whitespace "\n                  ")
             (:list
              (:symbol "name") (:whitespace " ")
              (:symbol "test-name")))
            (:whitespace "\n            ")
            (:list
             (:symbol "intern") (:whitespace " ")
             (:symbol "*ns*") (:whitespace " ")
             (:list
              (:symbol "symbol") (:whitespace " ")
              (:symbol "test-name")))))
          (:whitespace "\n        ")
          (:list
           (:symbol "sort") (:whitespace " ")
           (:list
            (:symbol "keys") (:whitespace " ")
            (:list
             (:symbol "ns-interns") (:whitespace " ")
             (:symbol "*ns*")))))))
       (:whitespace "\n  ")
       (:deref
        (:symbol "clojure.test/*report-counters*")))

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
           (rewrite/splice-after-ns-ish-form without-prep-forms
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
     (filter (fn [test-name]
       (re-matches #\"^test-at-line-.*\"
         (name test-name))))
     (run! (fn [test-name]
             (ns-unmap *ns* test-name))))

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
      (keep
        (fn [test-name]
          (when (re-matches #\"^test-at-line-.*\"
                  (name test-name))
            (intern *ns* (symbol test-name))))
        (sort (keys (ns-interns *ns*))))))
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
     (filter (fn [test-name]
       (re-matches #\"^test-at-line-.*\"
         (name test-name))))
     (run! (fn [test-name]
             (ns-unmap *ns* test-name))))

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
      (keep
        (fn [test-name]
          (when (re-matches #\"^test-at-line-.*\"
                  (name test-name))
            (intern *ns* (symbol test-name))))
        (sort (keys (ns-interns *ns*))))))
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
     (filter (fn [test-name]
       (re-matches #\"^test-at-line-.*\"
         (name test-name))))
     (run! (fn [test-name]
             (ns-unmap *ns* test-name))))

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
      (keep
        (fn [test-name]
          (when (re-matches #\"^test-at-line-.*\"
                  (name test-name))
            (intern *ns* (symbol test-name))))
        (sort (keys (ns-interns *ns*))))))
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
     (filter (fn [test-name]
       (re-matches #\"^test-at-line-.*\"
         (name test-name))))
     (run! (fn [test-name]
             (ns-unmap *ns* test-name))))

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
      (keep
        (fn [test-name]
          (when (re-matches #\"^test-at-line-.*\"
                  (name test-name))
            (intern *ns* (symbol test-name))))
        (sort (keys (ns-interns *ns*))))))
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
     (filter (fn [test-name]
       (re-matches #\"^test-at-line-.*\"
         (name test-name))))
     (run! (fn [test-name]
             (ns-unmap *ns* test-name))))

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
      (keep
        (fn [test-name]
          (when (re-matches #\"^test-at-line-.*\"
                  (name test-name))
            (intern *ns* (symbol test-name))))
        (sort (keys (ns-interns *ns*))))))
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
