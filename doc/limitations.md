# Limitations

* Only JVM Clojure so far.

* No testing has been done on .cljc files.

* The content of the source file may make a difference.  The specific
  cases that may work include syntactically correct source files
  with the following limits on content:

  * Exactly 1 `ns` form (with no forms before it), and no `in-ns` forms

  * Exactly 1 `in-ns` form (with no forms before it), and no `ns` forms

  * No `ns` nor `in-ns` forms

  Another way to put it might be that it's safest if there is at most
  one `ns` or `in-ns` form exactly and if one does exist, things are
  more likely to work if there isn't any code that comes before it.

  I believe most Clojure source files meet these conditions, but
  haven't yet performed a survey.

* The following sort of construct may not work:

  ```
  (comment

    (+ 1 1) ; don't put a comment here
    ;; => 2

  )
  ```

* There might be some difficulties placing comment block tests within
  files that are already "test" files.  No testing has been done for
  this use case.
