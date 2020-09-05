# alc.x-as-tests

## Purpose

Use a variety of things as tests.

Currently, one idea is to use [certain
constructs](doc/comment-block-tests.md) within Clojure's `comment`
blocks as tests, e.g.:

```
(comment

  (- 1 1)
  ;; => 0

)
```

## Status

Early stage.

## Prerequisites

* Running: clj / tools.deps(.alpha)
* Building executable (optional): Leiningen + Graalvm + native-image

## Use Cases and Limitations

[When](doc/use-cases.md) this might be useful, and the [fine
print](doc/limitations.md).

## Quick Trial

Suppose there is a Clojure project directory using `deps.edn`:

`~/a-clj-proj-dir`

and a source file with appropriate comment blocks at:

`~/a-clj-proj-dir/src/fun.clj`

Generate a file with tests for `fun.clj` and execute them by:

```
$ cd ~/a-clj-proj-dir
$ cat src/fun.clj | clj -Sdeps '{:deps {alc.x-as-tests {:git/url "https://github.com/sogaiu/alc.x-as-tests" :sha "a38ab51270113e5ec4b904f97a8fd9d96f2fd3a5"}}}' -m alc.x-as-tests.main | clj -
```

See [here](doc/lein-and-boot.md) for some more details including use
with Leiningen and/or Boot.

## General Setup and Use

alc.x-as-tests can be used via `clj` via appropriate configuration of
`deps.edn`.  It can also be used via a native-image binary `alc.xat`
(see below for building instructions).

To use via `clj`, first edit `~/.clojure/deps.edn` to have an alias
like:

```
...
:aliases
{
 :x-as-tests ; or :alc.x-as-tests
 {
  :extra-deps {sogaiu/alc.x-as-tests
                {:git/url "https://github.com/sogaiu/alc.x-as-tests"
                 :sha "a38ab51270113e5ec4b904f97a8fd9d96f2fd3a5"}}
  :main-opts ["-m" "alc.x-as-tests.main"]
 }
```

To generate a file with tests from a file (e.g. `src/fun.clj`) and
save it (e.g. as `fun-with-tests.clj`):

```
$ cat src/fun.clj | clj -A:x-as-tests > fun-with-tests.clj
```

or with the native-image binary:

```
$ cat src/fun.clj | alc.xat > fun-with-tests.clj
```

Note: `fun-with-tests.clj` is an ordinary Clojure file.

To run the tests (in e.g. `fun-with-tests.clj`) with `clj`:

```
$ clj -i fun-with-tests.clj
```

To generate and run in one invocation with `clj`:

```
$ cat src/fun.clj | clj -A:x-as-tests | clj -
```

or with the native-image binary:

```
$ cat src/fun.clj | alc.xat | clj -
```

See [here](doc/lein-and-boot.md) for hints on adopting these
invocations for use with Leiningen and/or Boot.

## Building

Building the native-image binary requires Leiningen and Graalvm.

### Linux and macos

With Leiningen installed and Graalvm 20.1.0 for Java 11 uncompressed
at `$HOME/src/graalvm-ce-java11-20.1.0`:

```
git clone https://github.com/sogaiu/alc.x-as-tests
cd alc.x-as-tests
export GRAALVM_HOME=$HOME/src/graalvm-ce-java11-20.1.0
export PATH=$GRAALVM_HOME/bin:$PATH
bash script/compile
```

This should produce a binary named `alc.xat`.  Putting this or a
symlink to it on `PATH` might make things more convenient.

### Windows 10

With Leiningen installed and Graalvm 20.1.0 for Java 11 uncompressed
at `C:\Users\user\Desktop\graalvm-ce-java11-20.1.0`, in a x64 Native
Tools Command Prompt:

```
git clone https://github.com/sogaiu/alc.x-as-tests
cd alc.x-as-tests
set GRAALVM_HOME=C:\Users\user\Desktop\graalvm-ce-java11-20.1.0
.\script\compile.bat
```

This should produce a binary named `alc.xat.exe`.  Putting this on
`PATH` might make things more convenient.

Note that on Windows, one of the usage invocations might be like:

```
C:\Users\user\Desktop\alc.x-as-tests> type src\alc\x_as_tests\impl\ast.clj | .\alc.xat.exe > ast-with-test.clj
```

## Technical Details

Curious about some [technical details](doc/technical-details.md)?  No?
That's why they are not on this page :)

## Related

* [judge-gen](https://github.com/sogaiu/judge-gen)

## Acknowledgments

* borkdude - babashka, clj-kondo, graalvm native-image work, pod-babashka-parcera, discussion, etc.
* carocad - parcera
* lread - clj-graal-docs and rewrite-cljc
* pyrmont - discussion
* Saikyun - discussion and testing
* taylorwood - code bits
