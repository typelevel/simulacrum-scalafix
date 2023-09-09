import sbtcrossproject.{CrossType, crossProject}
import ReleaseTransformations._

ThisBuild / organization := "org.typelevel"

val Scala212 = "2.12.18"

ThisBuild / crossScalaVersions := Seq(Scala212, "2.13.10", "3.3.1")
ThisBuild / scalaVersion := (ThisBuild / crossScalaVersions).value.head

ThisBuild / githubWorkflowPublishTargetBranches := Seq()

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List("test"),
    name = Some("Build and test all rules"),
    cond = Some(s"matrix.scala == '$Scala212'")),

  WorkflowStep.Sbt(
    List("annotation/test:compile", "annotationJS/test:compile"),
    name = Some("Build annotations"),
    cond = Some(s"matrix.scala != '$Scala212'")))

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

lazy val baseSettings = Seq(
  libraryDependencies ++= {
    if (scalaVersion.value.startsWith("3"))
      Nil
    else
      Seq(compilerPlugin(scalafixSemanticdb))
  },
  scalacOptions ++= {
    if (scalaVersion.value.startsWith("3"))
      Seq("-Ykind-projector")
    else
      compilerOptions
  },
  Compile / console / scalacOptions ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  Test / console / scalacOptions ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  }
)

lazy val allSettings = baseSettings ++ publishSettings

val metaSettings = Seq(crossScalaVersions := Seq(Scala212))

val testSettings = Seq(
  publish / skip := true,
  libraryDependencies ++= {
    if (scalaVersion.value.startsWith("3"))
      Nil
    else
      Seq(compilerPlugin(("org.typelevel" %% "kind-projector" % "0.13.2").cross(CrossVersion.full)))
  },
  libraryDependencies += "org.typelevel" %% "cats-kernel" % "2.10.0"
)

lazy val V = _root_.scalafix.sbt.BuildInfo

lazy val root = project
  .in(file("."))
  .settings(metaSettings)
  .settings(allSettings ++ noPublishSettings)
  .settings(
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      releaseStepCommandAndRemaining("+test"),
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      releaseStepCommandAndRemaining("+publishSigned"),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
  .aggregate(annotationJVM, annotationJS, rules, input, output, tests)

lazy val annotation = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("annotation"))
  .settings(allSettings)
  .settings(
    moduleName := "simulacrum-scalafix-annotations"
  )

lazy val annotationJVM = annotation.jvm
lazy val annotationJS = annotation.js

lazy val rules = project
  .settings(allSettings)
  .settings(metaSettings)
  .settings(
    moduleName := "simulacrum-scalafix",
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % V.scalafixVersion,
    scalacOptions += "-Ywarn-unused-import"
  )

lazy val input = project
  .settings(crossScalaVersions := crossScalaVersions.value.filter(_.startsWith("2.")))
  .settings(allSettings ++ testSettings)
  .dependsOn(annotationJVM)

lazy val output = project.disablePlugins(ScalafmtPlugin).settings(allSettings ++ testSettings).dependsOn(annotationJVM)

lazy val tests = project
  .settings(allSettings)
  .settings(metaSettings)
  .settings(
    publish / skip := true,
    libraryDependencies += ("ch.epfl.scala" % "scalafix-testkit" % V.scalafixVersion % Test).cross(CrossVersion.full),
    Compile / compile :=
      (Compile / compile).dependsOn(input / Compile / compile).value,
    scalafixTestkitOutputSourceDirectories :=
      (output / Compile / sourceDirectories).value,
    scalafixTestkitInputSourceDirectories :=
      (input / Compile / sourceDirectories).value,
    scalafixTestkitInputClasspath :=
      (input / Compile / fullClasspath).value
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
  Test / publishArtifact := false,
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
