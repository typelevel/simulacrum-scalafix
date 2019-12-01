# Simulacrum Scalafix experiment

[![Build status](https://img.shields.io/travis/travisbrown/simulacrum-scalafix/master.svg)](https://travis-ci.org/travisbrown/simulacrum-scalafix)
[![Coverage status](https://img.shields.io/codecov/c/github/travisbrown/simulacrum-scalafix/master.svg)](https://codecov.io/github/travisbrown/simulacrum-scalafix)

This project is an experiment in rewriting Typelevel [Simulacrum][simulacrum] as a set of
[Scalafix][scalafix] rules. It's currently a rough proof-of-concept, but it is able to replace
Simulacrum in the [Cats][cats] repository without breaking any tests or binary-compatibility checks.

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
[scalafix]: https://github.com/scalacenter/scalafix
[simulacrum]: https://github.com/typelevel/simulacrum
