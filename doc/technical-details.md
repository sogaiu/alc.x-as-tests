# Technical Details

All of the heavy-lifting is done by
[parcera](https://github.com/carocad/parcera).

The parcera library is used to parse a string of Clojure source code
to produce an AST that includes information about whitespace and
comments.  The AST is then transformed and parcera is used again to
produce another string of Clojure source code.

The transformation basically consists of taking forms within comment
blocks and wrapping them in forms to express tests.  Forms are also
inserted near the "beginning" and "end" in order to arrange for
appropriate test execution.

At the moment, pieces of `clojure.test` are used to express tests as
well as execute them and report results.
