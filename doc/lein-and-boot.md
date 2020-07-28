# Leiningen and Boot

In general, the Clojure process that is going to run the tests may
need to have an appropriate classpath in order to satisfy any
dependencies.

So for a Leiningen project, the following might be a relatively
comparable quick trial invocation:

```
$ cd ~/a-clj-proj-dir
$ cat src/fun.clj | clj -Sdeps '{:deps {alc.x-as-tests {:git/url "https://github.com/sogaiu/alc.x-as-tests" :sha "d4b001fe5abe8f1c7b2244f17001064fad1c5135"}}}' -m alc.x-as-tests.main | lein repl
```

Not as pretty perhaps (see note below about `lein exec`).

I didn't find a comparable way to invoke Boot, but it's possible to
save the output of the transformation step above to a file and pass
that as an argument to `boot` using `-f`.

For example, first create `fun-with-tests.clj` to contain transformed
content with tests:

```
$ cd ~/a-clj-proj-dir
$ cat src/fun.clj | clj -Sdeps '{:deps {alc.x-as-tests {:git/url "https://github.com/sogaiu/alc.x-as-tests" :sha "d4b001fe5abe8f1c7b2244f17001064fad1c5135"}}}' -m alc.x-as-tests.main > fun-with-tests.clj
```

Then:

```
boot -f fun-with-tests.clj
```

Incidentally, that can sort of thing can be done with `clj` too:

```
clj -i fun-with-tests.clj
```

For Leiningen, [lein exec](https://github.com/kumarshantanu/lein-exec)
provides invocation methods that may work with both types of methods
in a "prettier" way.  This has not been tested though.
