;; TODO
;;
;; XXX: some large values are more readable formatted.  unfortunately,
;;      this can be manual work.  it would be nice to have a more
;;      automated way to achieve formatting.  try to think of some
;;      ways to achieve this.
;;
;; XXX: there may be other issues noted as comments in the source.  look for
;;      "XXX"

;; QUESTIONS:
;;
;; XXX: support command line options?  can experiment via a single
;;      "map".  possible options include: input file path, output file
;;      path.
;;
;; XXX: support exceptions as expected values?  come up with notation
;;      to express this.
;;
;; XXX: should there be an initial "analysis" that leads to early
;;      termination?  such an analysis might check if there any of
;;      the following conditions are true:
;;
;;      1) there is more than one ns form
;;      2) there is more than one in-ns form
;;      3) both ns and in-ns forms exist
;;      4) deftest or other test-like forms exist (tricky?)
;;
;;      this motivates the idea of "directives" to instruct
;;      specifically where to place the clojure.test require and old
;;      test erasing forms.
;;
;; XXX: consider supporting "directives" -- e.g. comment blocks that
;;      contain particular sequences that affect the "transformation".
;;      one possible application is to indicate where to place the
;;      require form for clojure.test.  another obvious one might be
;;      something to indicate where test execution should happen.
;;      these sorts of things might allow more fine-grained control of
;;      the "transformation" to handle situations where there is more
;;      than one ns form or there are no ns forms (and say e.g. only
;;      an in-ns form).  there could be special "directives" within
;;      comment blocks to indicate what mode of processing to
;;      use (e.g. no such directives gives default behavior (insert
;;      require and run-tests), but existence of a particular one
;;      might turn off the default behavior).
;;
;; XXX: support doctests?
;;
;; XXX: rename some things in ast to end in "-node"?
;;      e.g. whitespace-node?
;;
;; XXX: test if metadata is handled in actual / expected expressions?
;;
;; XXX: replace use of parcera nodes (e.g. [:whitespace " "]) with
;;      results of calling pc/ast (e.g. (pc/ast " ")) in implementations
;;      (not necessarily tests in comment blocks) as this will shield
;;      the code from parcera changes.  is this a relevant concern?
;;      this has been addressed, but not within comment block tests.
;;      not clear whether they should also not refer to parcera
;;      nodes (e.g. [:whitespace " "]).
;;
;; XXX: arranging for approriate whitespace for output seems to make
;;      things kind of complex.  try to postpone addressing them to
;;      the end or ignore them when testing (via some kind of
;;      normalization?)?  since there are depdencies anyway, would
;;      including zprint, cljfmt, cljstyle, or similar and using that
;;      sort of thing to format the results be worth it?  zprint in
;;      default mode didn't seem to help.  cljstyle seems promising.
;;      have slightly improved whitespace situation, so waiting to
;;      see if anything more needs to be done.
;;
;; XXX: when an expected value is a long string, it is cumbersome to
;;      see and thus manually verify, edit, etc.  using multi-line
;;      strings for the moment -- quoting is necessary in some places,
;;      but it's an improvement over the previous situation.  a
;;      downside is they need to be printed first, so there is a bit
;;      of tedium, but waiting to see if it is tolerable.
;;
;; XXX: printing results out seems to be helpful when manually
;;      verifying some kinds of results.  for automation, printed
;;      results aren't obviously available?  isn't there something
;;      like "print" that returns the value that it prints?

;; OBSERVATIONS
;;
;; * parcera returns things that look almost like hiccup
;;
;; * parcera/code can be fed hiccup
;;
;; * the keywords in nodes seem verbose -- :whitespace vs :ws
;;
;; * values like: (:list (:symbol "def") ...) might be inconvenient to
;;   express as "expected" values (eval-ing them leads to errors)
;;   converting them to use vector notation is a work-around, but for
;;   nested things seems rather more work than one might like.  using
;;   a single quote at the beginning seems to work ok.
;;
;; * comment blocks within comment blocks remain comment blocks after
;;   transformation.  this can be seen as a feature to have things
;;   that one only evaluates via the repl.

(ns alc.x-as-tests.main
  (:require
   [alc.x-as-tests.impl.rewrite :as rewrite])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn -main
  [& _]
  (let [slurped (slurp *in*)]
    (print (rewrite/rewrite-with-tests slurped)))
  (flush)
  (System/exit 0))
