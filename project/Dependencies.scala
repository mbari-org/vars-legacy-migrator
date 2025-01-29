import sbt._

object Dependencies {

  lazy val annosaurusSdk = "org.mbari.vars" % "annosaurus-java-sdk" % "0.0.1"

  private val circeVersion = "0.14.10"
  lazy val circeCore       = "io.circe" %% "circe-core"    % circeVersion
  lazy val circeGeneric    = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser     = "io.circe" %% "circe-parser"  % circeVersion

  lazy val mainargs = "com.lihaoyi"                 %% "mainargs"        % "0.7.6"
  lazy val methanol = "com.github.mizosoft.methanol" % "methanol"        % "1.8.0"
  lazy val munit    = "org.scalameta"               %% "munit"           % "1.1.0"
  lazy val scommons = "org.mbari.commons"           %% "scommons"        % "0.0.7"
  lazy val typesafeConfig = "com.typesafe"   % "config"          % "1.4.3"
  lazy val varsLegacy = "org.mbari.vars" % "vars-legacy" % "6.0.0"


}
