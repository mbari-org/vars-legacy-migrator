import Dependencies._

Global / onChangedBuildSource := ReloadOnSourceChanges
Compile / doc / scalacOptions ++= Seq(
    "-groups",
    "-project-footer",
    "Monterey Bay Aquarium Research Institute",
    "-siteroot",
    "src/docs",
    "-doc-root-content",
    "./src/docs/index.md"
)

ThisBuild / scalaVersion     := "3.6.3"
ThisBuild / organization     := "org.mbari"
ThisBuild / organizationName := "MBARI"
ThisBuild / startYear        := Some(2025)
ThisBuild / versionScheme    := Some("semver-spec")

lazy val root = project
  .in(file("."))
  .enablePlugins(
    AutomateHeaderPlugin,
    GitBranchPrompt,
    GitVersioning,
    JavaAppPackaging
  )
  .settings(
    name := "vars-migration",
    // Set version based on git tag. I use "0.0.0" format (no leading "v", which is the default)
    // Use `show gitCurrentTags` in sbt to update/see the tags
    git.gitTagToVersionNumber := { tag: String =>
      if(tag matches "[0-9]+\\..*") Some(tag)
      else None
    },
    git.useGitDescribe := true,
    // sbt-header
    headerLicense := Some(
      HeaderLicense.Custom(
        """Copyright (c) Monterey Bay Aquarium Research Institute 2022
        |
        |vars-migration code is non-public software. Unauthorized copying of this file,
        |via any medium is strictly prohibited. Proprietary and confidential.
        |""".stripMargin
      )
    ),
    javacOptions ++= Seq("-target", "21", "-source", "21"),
    libraryDependencies ++= Seq(
      annosaurusSdk,
      circeCore,
      circeGeneric,
      circeParser,
      methanol,
      munit          % Test,
      mainargs,
      typesafeConfig,
      varsLegacy
    ),
    scalacOptions ++= Seq(
      "-deprecation", // Emit warning and location for usages of deprecated APIs.
      "-encoding",
      "UTF-8",        // yes, this is 2 args. Specify character encoding used by source files.
      "-feature",     // Emit warning and location for usages of features that should be imported explicitly.
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-unchecked",
      "-Vprofile"
    )
  )

// https://stackoverflow.com/questions/22772812/using-sbt-native-packager-how-can-i-simply-prepend-a-directory-to-my-bash-scrip
bashScriptExtraDefines ++= Seq(
  """addJava "-Dconfig.file=${app_home}/../conf/application.conf"""",
  """addJava "-Dlogback.configurationFile=${app_home}/../conf/logback.xml""""
)
batScriptExtraDefines ++= Seq(
  """call :add_java "-Dconfig.file=%APP_HOME%\conf\application.conf"""",
  """call :add_java "-Dlogback.configurationFile=%APP_HOME%\conf\logback.xml""""
)
