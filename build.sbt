import sbtcrossproject.{CrossType, crossProject}
import ReleaseTransformations._

ThisBuild / organization := "org.typelevel"

val Scala212 = "2.12.13"

ThisBuild / crossScalaVersions := Seq(Scala212, "2.13.3", "3.0.0-M2", "3.0.0-M3")
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
    if (isDotty.value)
      Nil
    else
      Seq(compilerPlugin(scalafixSemanticdb))
  },
  scalacOptions ++= { if (isDotty.value) Seq("-Ykind-projector") else compilerOptions },
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  coverageHighlighting := true,
  Compile / doc / sources := {
    val old = (Compile / doc / sources).value
    if (isDotty.value)
      Seq()
    else
      old
  }
)

lazy val allSettings = baseSettings ++ publishSettings

val metaSettings = Seq(crossScalaVersions := Seq(Scala212))

val testSettings = Seq(
  skip in publish := true,
  libraryDependencies ++= {
    if (isDotty.value)
      Nil
    else
      Seq(compilerPlugin(("org.typelevel" %% "kind-projector" % "0.11.3").cross(CrossVersion.full)))
  },
  libraryDependencies += ("org.typelevel" %% "cats-kernel" % "2.4.1").withDottyCompat(scalaVersion.value)
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
  .jsSettings(
    coverageEnabled := false
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
