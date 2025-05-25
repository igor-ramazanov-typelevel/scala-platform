import org.typelevel.sbt.gha.WorkflowStep.Run
import org.typelevel.sbt.gha.WorkflowStep.Sbt

val scala213 = "2.13.16"
val scala3   = "3.3.6"

ThisBuild / githubOwner                    := "igor-ramazanov-typelevel"
ThisBuild / githubRepository               := "scala-platform"
ThisBuild / githubWorkflowPublishPreamble  := List.empty
ThisBuild / githubWorkflowUseSbtThinClient := true
ThisBuild / githubWorkflowPublish := List(
  Run(
    commands = List("echo \"$PGP_SECRET\" | gpg --import"),
    id = None,
    name = Some("Import PGP key"),
    env = Map("PGP_SECRET" -> "${{ secrets.PGP_SECRET }}"),
    params = Map(),
    timeoutMinutes = None,
    workingDirectory = None
  ),
  Sbt(
    commands = List("+ publish"),
    id = None,
    name = Some("Publish"),
    cond = None,
    env = Map("GITHUB_TOKEN" -> "${{ secrets.GB_TOKEN }}"),
    params = Map.empty,
    timeoutMinutes = None,
    preamble = true
  )
)
ThisBuild / gpgWarnOnFailure := false

ThisBuild / scalaVersion       := scala213
ThisBuild / crossScalaVersions := Seq(scala213, scala3)
ThisBuild / tlBaseVersion      := "1.0"

// publishing info
inThisBuild(
  Seq(
    organization  := "lgbt.princess",
    versionScheme := Some("early-semver"),
    homepage      := Some(url("https://github.com/NthPortal/scala-platform")),
    licenses      := Seq(License.Apache2),
    developers := List(
      Developer(
        "NthPortal",
        "Marissa",
        "dev@princess.lgbt",
        url("https://github.com/NthPortal"),
      ),
    ),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/NthPortal/scala-platform"),
        "scm:git:git@github.com:NthPortal/scala-platform.git",
        "scm:git:git@github.com:NthPortal/scala-platform.git",
      ),
    ),
  ),
)

// CI config
inThisBuild(
  Seq(
    githubWorkflowTargetTags ++= Seq("v*"),
    githubWorkflowPublishTargetBranches ++= Seq(
      RefPredicate.StartsWith(Ref.Tag("v")),
    ),
    githubWorkflowJavaVersions := Seq(JavaSpec.temurin("8")),
    githubWorkflowBuildPostamble ++= Seq(
      WorkflowStep.Sbt(
        name = Some("scalafmt"),
        commands = List("scalafmtCheckAll", "scalafmtSbtCheck"),
      ),
    ),
    githubWorkflowBuildMatrixFailFast := Some(false),
  ),
)

lazy val platform =
  crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .in(file("platform"))
    .settings(
      name := "platform",
      scalacOptions ++= Seq(
        "-feature",
        "-Werror",
      ),
      scalacOptions ++= {
        CrossVersion.partialVersion(scalaVersion.value) match {
          case Some((2, _)) => Seq("-Xlint:_")
          case _            => Nil
        }
      },
      libraryDependencies ++= Seq(
        "org.scalameta" %%% "munit" % "1.1.1" % Test,
      ),
      mimaFailOnNoPrevious      := false,
      publishTo                 := githubPublishTo.value,
      publishConfiguration      := publishConfiguration.value.withOverwrite(true),
      publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
    )
