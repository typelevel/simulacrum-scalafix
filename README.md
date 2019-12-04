# Simulacrum Scalafix experiment

[![Build status](https://img.shields.io/travis/travisbrown/simulacrum-scalafix/master.svg)](https://travis-ci.org/travisbrown/simulacrum-scalafix)
[![Coverage status](https://img.shields.io/codecov/c/github/travisbrown/simulacrum-scalafix/master.svg)](https://codecov.io/github/travisbrown/simulacrum-scalafix)

This project is an experiment in rewriting Typelevel [Simulacrum][simulacrum] as a set of
[Scalafix][scalafix] rules. It's currently a rough proof-of-concept, but it is able to replace
Simulacrum in the [Cats][cats] repository without breaking any tests or binary-compatibility checks.

## Rules

Simulacrum 1's `@typeclass` macro annotation adds three kinds of boilerplate, which this project
factors into three Scalafix rules:

* `AddSerializable`: This rule adds `extends Serializable` to the root type classes in a type class hierarchy.
* `AddImplicitNotFound`: This rule adds custom `@implicitNotFound` annotations for type classes.
* `TypeClassSupport`: This rule adds a summoner method and syntax method support to the type class companion object.

This repo currently includes a few miscellaneous Scalafix rules that may be useful for experimenting
with Dotty cross-compilation:

* `ExpandTypeLambdas`: This rule rewrites (a large subset of) [kind-projector][kind-projector]'s
  type lambda syntax to a representation that currently works on both Scala 2 and Dotty.
* `ExpandPolymorphicLambdas`: This rule rewrites kind-projector's polymorphic lambda values to
  explicit anonymous class instantiations that work on both Scala 2 and Dotty.
* `ParenthesizeLambdaParamWithType`: This rule parenthesizes lambda parameters with type annotations
  as needed.

The kind-projector-related `Expand` rules are not intended to be a long-term solution. The issue is
that right now Dotty has its own syntax for type lambdas and polymorphic function values, and it
doesn't support kind-projector's syntax (despite the existence of a `-Ykind-projector` compiler
option, which currently doesn't actually do anything), while kind-projector doesn't support Dotty's
syntax on Scala 2.

If you want to cross-compile for Scala 2 and Dotty _right now_, you need to use an extremely verbose
encoding of type lambdas and polymorphic function values, which is what the rules in this repo
target. In the future this won't be necessary, either because Dotty will add support for
kind-projector syntax via its `-Ykind-projector` flag, or because kind-projector will add support
for Dotty syntax, or some of both (or something else entirely). In any case you almost certainly
don't want to use the verbose encoding you'll get from these `Expand` rules outside experimental
branches.

## License

This experimental code is licensed under the **[Apache License, Version 2.0][apache]**
(the "License"); you may not use this software except in compliance with the
License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[apache]: http://www.apache.org/licenses/LICENSE-2.0
[cats]: https://github.com/typelevel/cats
[code-of-conduct]: https://www.scala-lang.org/conduct.html
[contributing]: https://circe.github.io/circe/contributing.html
[kind-projector]: https://github.com/typelevel/kind-projector
[scalafix]: https://github.com/scalacenter/scalafix
[simulacrum]: https://github.com/typelevel/simulacrum
