val commonSettings = List(
  addCompilerPlugin(Versions.kindProjector),
  organization := "com.alexknvl",
  version      := "0.0.2",
  licenses     += ("MIT", url("http://opensource.org/licenses/MIT")),
  scalaVersion := "2.12.2",
  scalacOptions ++= List(
    "-deprecation", "-unchecked", "-feature",
    "-encoding", "UTF-8",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Xfuture",
    "-language:higherKinds"),
  libraryDependencies
    ++= Versions.cats
    ++  Versions.testing,
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
    Resolver.mavenLocal))


lazy val commonJvmSettings = Seq(
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oDF"))

lazy val commonJsSettings = Seq(
  scalaJSStage in Global := FastOptStage,
  parallelExecution := false,
  requiresDOM := false,
  jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv(),
  // batch mode decreases the amount of memory needed to compile scala.js code
  scalaJSOptimizerOptions := scalaJSOptimizerOptions.value.withBatchMode(scala.sys.env.get("TRAVIS").isDefined))

lazy val root = project.in(file("."))
  .settings(commonSettings: _*)
  .aggregate(newtypesJVM, newtypesJS)

lazy val random = crossProject.crossType(MyCrossType)
  .in(file("."))
  .settings(name       := "random")
  .settings(moduleName := "random")
  .settings(commonSettings: _*)
  .jsSettings(commonJsSettings:_*)
  .jvmSettings(commonJvmSettings:_*)

lazy val newtypesJVM = random.jvm
lazy val newtypesJS  = random.js
