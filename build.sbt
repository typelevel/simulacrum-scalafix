import sbtcrossproject.{CrossType, crossProject}

organization in ThisBuild := "org.typelevel"

val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Yrangepos",
  "-P:semanticdb:synthetics:on"
)

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }

lazy val baseSettings = Seq(
  scalaVersion := "2.12.10",
  addCompilerPlugin(scalafixSemanticdb),
  scalacOptions ++= compilerOptions,
  scalacOptions ++= (
    if (priorTo2_13(scalaVersion.value))
      Seq(
        "-Xfuture",
        "-Yno-adapted-args",
        "-Ywarn-unused-import"
      )
    else
      Seq(
        "-Ywarn-unused:imports"
      )
  ),
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  coverageHighlighting := true
)

lazy val allSettings = baseSettings ++ publishSettings

val testSettings = Seq(
  crossScalaVersions := Seq(scalaVersion.value),
  skip in publish := true,
  addCompilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.0").cross(CrossVersion.full)),
  libraryDependencies += "org.typelevel" %% "cats-kernel" % "2.1.1"
)

lazy val V = _root_.scalafix.sbt.BuildInfo

lazy val root = project
  .in(file("."))
  .settings(allSettings ++ noPublishSettings)
  .aggregate(annotationJVM, annotationJS, rules, input, output, tests)

lazy val annotation = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("annotation"))
  .settings(allSettings)
  .settings(
    crossScalaVersions := Seq(scalaVersion.value, "2.13.1"),
    moduleName := "simulacrum-scalafix-annotations"
  )
  .jsSettings(
    coverageEnabled := false
  )

lazy val annotationJVM = annotation.jvm
lazy val annotationJS = annotation.js

lazy val rules = project
  .settings(allSettings)
  .settings(
    crossScalaVersions := Seq(scalaVersion.value),
    moduleName := "simulacrum-scalafix",
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % V.scalafixVersion,
    scalacOptions += "-Ywarn-unused-import"
  )

lazy val input = project.settings(allSettings ++ testSettings).dependsOn(annotationJVM)

lazy val output = project.disablePlugins(ScalafmtPlugin).settings(allSettings ++ testSettings).dependsOn(annotationJVM)

lazy val tests = project
  .settings(allSettings)
  .settings(
    crossScalaVersions := Seq(scalaVersion.value),
    skip in publish := true,
    libraryDependencies += ("ch.epfl.scala" % "scalafix-testkit" % V.scalafixVersion % Test).cross(CrossVersion.full),
    compile.in(Compile) :=
      compile.in(Compile).dependsOn(compile.in(input, Compile)).value,
    scalafixTestkitOutputSourceDirectories :=
      sourceDirectories.in(output, Compile).value,
    scalafixTestkitInputSourceDirectories :=
      sourceDirectories.in(input, Compile).value,
    scalafixTestkitInputClasspath :=
      fullClasspath.in(input, Compile).value
  )
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseVcsSign := true,
  homepage := Some(url("https://github.com/typelevel/simulacrum-scalafix")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  autoAPIMappings := true,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/typelevel/simulacrum-scalafix"),
      "scm:git:git@github.com:typelevel/simulacrum-scalafix.git"
    )
  ),
  developers := List(
    Developer(
      "travisbrown",
      "Travis Brown",
      "travisrobertbrown@gmail.com",
      url("https://twitter.com/travisbrown")
    )
  )
)
