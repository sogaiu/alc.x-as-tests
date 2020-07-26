# Comment Block Tests

* The source code for alc.x-as-tests uses these types of tests.  There
  are plenty of examples there :)

* Within `comment` blocks, put expressions / forms to be tested along
  with brief records of expected values:

  ```
  (comment

    (- 1 1)
    ;; => 0

  )
  ```

  Note the use of a line comment and `=>` to express an expected
  return value.

  Note also that for the purposes of expressing an expected value, the
  line comment requires 2 semi-colons.


* Use `[]` instead of `()` to express return values involving lists,
  e.g.

  ```
  (list :hi 1)
  ;; => [:hi 1]
  ```

  not:

  ```
  (list :hi 1)
  ;; => (:hi 1)
  ```

  Using `quote` / `'` might work better for nested things:

  ```
  (cons :hi '((:i (:am (:feeling (:nested 8))))))
  ;; => '(:hi (:i (:am (:feeling (:nested 8)))))
  ```

* Express return values that span multiple lines by using
  [discard](https://clojure.org/guides/weird_characters#_discard)
  `#_`:

  ```
  (comment

    (parcera.core/ast "(require 'clojure.test)"
    #_ '(:code
          (:list
            (:symbol "require") (:whitespace " ")
            (:quote (:symbol "clojure.test"))))

  )
  ```

  Note the use of `'` as suggested earlier.

* More than one expression + expected value info pair (i.e. a test)
  can be placed in a comment block, so:

  ```
  (comment

    (+ 1 1)
    ;; => 2

    (- 1 1)
    ;; => 0
  )
  ```

  will yield two tests.

* It's fine to put other forms in the comment block that don't have
  expected value info appearing after them.  All forms in comment
  blocks will be included in tests (though see note below about nested
  comment forms), thus:

  ```
  (comment

    (def a 1)

    (+ a 1)
    ;; => 2
  )
  ```

  will yield one test that includes the `def` invocation.

* When there is more than one "test" and there are non-test forms, e.g.:

  ```
  (comment

    (def a 1)

    (+ a 1)
    ;; => 2

    (- a 1)
    ;; => 0
  )
  ```

  This will yield two tests and the `def` invocation will remain part
  of the first test.  However, at execution time, tests will be
  executed in the order in which they were "defined", so the `def`
  invocation will be effective for the second test (i.e. `(- a 1)`).

* Nested comment blocks will remain "commented".  This is a "feature"
  to allow expression of tests within comment blocks one might want to
  execute manually but not automatically.  Basically nested comment
  blocks will not be treated as tests.

* See [limitations](limitations.md) for information on some
  constraints about what types of Clojure source code this might work
  with.
