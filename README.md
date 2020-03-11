# Simulacrum Scalafix

[![Build status](https://img.shields.io/travis/typelevel/simulacrum-scalafix/master.svg)](https://travis-ci.org/typelevel/simulacrum-scalafix)
[![Coverage status](https://img.shields.io/codecov/c/github/typelevel/simulacrum-scalafix/master.svg)](https://codecov.io/github/typelevel/simulacrum-scalafix)
[![Maven Central](https://img.shields.io/maven-central/v/org.typelevel/simulacrum-scalafix-annotations_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/org.typelevel/simulacrum-scalafix-annotations_2.13)

This project began as an experiment in rewriting Typelevel [Simulacrum][simulacrum] as a set of
[Scalafix][scalafix] rules. It's currently still a proof-of-concept, but it is able to replace
Simulacrum 1 in the [Cats][cats] repository without breaking any tests or binary-compatibility checks.

Please see [this Cats issue](https://github.com/typelevel/cats/issues/3192) for discussion about this experiment.

Development of this tool has been supported by [Permutive][permutive]. Please see our [monthly
reports][permutive-medium] for updates on the work of the Permutive Community Engineering team.

## Simulacrum rules

Simulacrum 1's `@typeclass` macro annotation adds three kinds of boilerplate, which this project
factors into three Scalafix rules:

* `AddSerializable`: This rule adds `extends Serializable` to the root type classes in a type class hierarchy.
* `AddImplicitNotFound`: This rule adds custom `@implicitNotFound` annotations for type classes.
* `TypeClassSupport`: This rule adds a summoner method and syntax method support to the type class companion object.

## Dotty compatibility rules

This repo currently includes a few miscellaneous Scalafix rules that may be useful for experimenting
with Dotty cross-compilation:

* `ExpandPolymorphicLambdas`: This rule rewrites kind-projector's polymorphic lambda values to
  explicit anonymous class instantiations that work on both Scala 2 and Dotty.
* `ExpandTypeLambdas`: This rule rewrites (a large subset of) [kind-projector][kind-projector]'s
  type lambda syntax to a representation that currently works on both Scala 2 and Dotty.
* `ParenthesizeLambdaParamWithType`: This rule parenthesizes lambda parameters with type annotations
  as needed.

The first rule [may be used in Cats](https://github.com/typelevel/cats/pull/3193)—it's not yet clear
whether the polymorphic SAM support that may eventually be added to Dotty will support
cross-building there.

The latter two rules should no longer be necessary in most cases, since Dotty
[now supports][dotty-kind-projector] a subset of kind-projector's syntax via the `-Ykind-projector`
compiler option, and can add parentheses to lambda parameters with type annotations with
`-language:Scala2Compat -rewrite`.


## Building Cats without macro annotations

It's not currently possible to build Cats with [Dotty][dotty] because Simulacrum 1 uses macro
annotations, which Dotty doesn't support. The goal of this project is to change that by providing a
non-macro-annotation-based version of Simulacrum.

I have a Cats [branch](https://github.com/travisbrown/cats/tree/demo/simulacrum-scalafix) that
demonstrates how these Scalafix rules work. You can follow along with the following steps:

1. Add Scalafix and the locally-published Scalafix rules to the Cats build and remove Simulacrum 1, Macro Paradise, etc.
   This takes [about eight lines](https://github.com/travisbrown/cats/commit/4f6d5c25892e5b07c57b1a8980eb8fc9d0869dae)
   of configuration.
2. Add `@noop` annotations to `FlatMap#ifM`, `Functor#ifF`, and `Monad#whileM` and `whileM_`.
   [This](https://github.com/travisbrown/cats/commit/69d91a0d4ec5987132b936ad6b24351dbd2ee3f8) is necessary for
   compatibility because Simulacrum 1 didn't handle these methods.
3. Open an sbt console in the Cats repo and run the following commands:
   ```
   sbt:cats> scalafix AddSerializable
   sbt:cats> scalafix AddImplicitNotFound
   sbt:cats> scalafix TypeClassSupport
   sbt:cats> scalafmtAll
   ```
   This will result in [some boilerplate](https://github.com/travisbrown/cats/commit/a891d38104bfcc585b22a2c8ed65602fb4e13155) being added to the Cats source files:
   ```
   50 files changed, 2461 insertions(+), 17 deletions(-)
   ```
4. Run `sbt validateJVM` to verify that tests and binary-compatibility checks pass after the change
   (and `sbt ++2.13.1 buildJVM` if you want to check Scala 2.13 as well).

## Cross-building cats-core on Dotty

The instructions below are not up-to-date with Cats master, although they should still work.
Please follow [this work-in-progress PR](https://github.com/typelevel/cats/pull/3269) for the
current status of Dotty cross-building.

You can add Dotty cross-building with a few additional steps:

1. Add build configuration for Dotty. This is [a net couple dozen lines](https://github.com/travisbrown/cats/commit/53165d502d03e5e8f8555a89aad066ebcb9eb943).
2. [Move](https://github.com/travisbrown/cats/commit/f304311574ec05d16f4cf8139bffc9e64c833829) the one macro definition in cats-core into a Scala 2-specific source directory tree.
3. Expand type lambdas and polymorphic function value definitions with the following commands:
   ```
   sbt:cats> +scalafix ExpandTypeLambdas
   sbt:cats> +scalafix ExpandPolymorphicLambdas
   sbt:cats> +scalafmtAll
   ```
   This will result in [a pretty big diff](https://github.com/travisbrown/cats/commit/ef1c4824564a5660e0faa54b8a05e934136d84ac).
   Unlike the similar Simulacrum boilerplate expansion we did above, this kind-projector expansion
   probably isn't something we'd ever want to merge—it's just a convenient way to try out Dotty
   cross-building.
4. Add [a couple of casts](https://github.com/travisbrown/cats/commit/0ba32c7f873a59f3088d54ac6751e3a0aa0c952a) that Dotty needs for some reason.
5. Compile on Dotty:
   ```
   sbt:cats> ++0.21.0-bin-20191201-65a404f-NIGHTLY
   sbt:cats> coreJVM/compile
   sbt:cats> kernelLawsJVM/compile
   sbt:cats> alleycatsCoreJVM/compile
   ```
   These modules should compile without errors (the other laws modules and cats-free will currently fail).

## Community

People are expected to follow the [Scala Code of Conduct][code-of-conduct] on GitHub and in any
other project channels.

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
[dotty]: https://dotty.epfl.ch/
[dotty-kind-projector]: https://github.com/lampepfl/dotty/pull/7775
[kind-projector]: https://github.com/typelevel/kind-projector
[permutive]: https://permutive.com/
[permutive-medium]: https://medium.com/permutive
[scalafix]: https://github.com/scalacenter/scalafix
[simulacrum]: https://github.com/typelevel/simulacrum
