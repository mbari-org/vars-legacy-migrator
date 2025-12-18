import sbt.*

object Dependencies {

    lazy val annosaurusSdk   = "org.mbari.vars"               % "annosaurus-java-sdk"        % "0.0.8"
    private val circeVersion = "0.14.15"
    lazy val circeCore       = "io.circe"                    %% "circe-core"                 % circeVersion
    lazy val circeGeneric    = "io.circe"                    %% "circe-generic"              % circeVersion
    lazy val circeParser     = "io.circe"                    %% "circe-parser"               % circeVersion
    lazy val logback         = "ch.qos.logback"               % "logback-classic"            % "1.5.22"
    lazy val mainargs        = "com.lihaoyi"                 %% "mainargs"                   % "0.7.7"
    lazy val methanol        = "com.github.mizosoft.methanol" % "methanol"                   % "1.8.4"
    lazy val munit           = "org.scalameta"               %% "munit"                      % "1.2.1"
    lazy val oniSdk          = "org.mbari.vars"               % "oni-java-sdk"               % "0.0.4"
    lazy val razielSdk       = "org.mbari.vars"               % "raziel-java-sdk"            % "0.0.2"
    lazy val scommons        = "org.mbari.commons"           %% "scommons"                   % "0.0.7"
    lazy val slf4jSystem     = "org.slf4j"                    % "slf4j-jdk-platform-logging" % "2.0.17"
    lazy val sqlserver       = "com.microsoft.sqlserver"      % "mssql-jdbc"                 % "12.10.2.jre11"
    lazy val typesafeConfig  = "com.typesafe"                 % "config"                     % "1.4.5"
    lazy val vampireSquidSdk = "org.mbari.vars"               % "vampire-squid-java-sdk"     % "0.0.12"
    lazy val varsLegacy      = "org.mbari.vars"               % "vars-legacy"                % "6.0.0"
}
