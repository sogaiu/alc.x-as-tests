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

* Parinfer seems to insist on moving the closing paren of a comment
  block to a location that causes problems.  For example, before
  parinfer use, a comment block might look like:

  ```
  (comment

    (+ 1 1)
    ;; => 2

  )
  ```
  After parinfer use, the code might look like:
  ```
  (comment

    (+ 1 1))
    ;; => 2
  ```
  Note that the comment block's closing parenthesis has been moved so
  that it is now before the line comment `;; => 2` which was expressing
  an expected value.

  To work around this, one can apply a "door stop" technique
  (mentioned on #parinfer in slack).
  ```
  (comment

    (+ 1 1)
    ;; => 2

   ,)      <-- the comma seems to help
  ```
  The gist is to use something to "pin" the closing paren to remain
  after any line comments (which might be expressing expected values).

  The original suggestions encountered were to use: `[]` or `#__`.
  The former may work fine, but the latter may be problematic as it
  might get interpreted itself as expressing the idea that `_` is an
  expected value, thus it is not recommended.

  It appears that among some comment-block using folks, the use of a
  "door stop" is a thing, so perhaps this is not too problematic.
