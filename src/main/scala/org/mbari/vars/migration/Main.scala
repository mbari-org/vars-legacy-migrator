/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration

import mainargs.{arg, ParserForMethods, TokensReader}
import org.mbari.scommons.etc.jdk.Loggers.given
import org.mbari.vars.annosaurus.sdk.r1.AnnotationService
import org.mbari.vars.migration.etc.mainargs.PathReader
import org.mbari.vars.migration.model.MediaFactory
import org.mbari.vars.migration.services.ServiceBuilder
import org.mbari.vars.migration.subcommands.{Configure, Login, MigrateAll, MigrateOne, Preview, ServiceHealth}
import org.mbari.vars.vampiresquid.sdk.r1.MediaService
import vars.ToolBelt

import java.lang.System.Logger.Level
import java.nio.file.Path

object Main:

    // Needed for mainargs to parse Path arguments
    given TokensReader.Simple[Path] = PathReader

    private val log = System.getLogger(Main.getClass.getName)

    // Initialize the JDBC drivers
    val driversToLoad = List(
        "org.postgresql.Driver",
        "com.microsoft.sqlserver.jdbc.SQLServerDriver",
        "net.sourceforge.jtds.jdbc.Driver"
    )
    driversToLoad.foreach(Class.forName)

    private val toolBeltOption = Configure.load()
    private val serviceBuilder = ServiceBuilder(true)
    private val mediaFactory   = MediaFactory.load()

    def main(args: Array[String]): Unit =
        ParserForMethods(this).runOrExit(args.toSeq)
        System.exit(0)

    @mainargs.main(
        name = "login",
        doc = "Login to raziel, the VARS API gateway"
    )
    def login(
        @arg(positional = true, doc = "URL to Raziel (e.g. https://m3.shore.mbari.org/config)") razielUrl: String
    ): Unit =
        log.atInfo.log("Logging in to Raziel services")
        Login.run(razielUrl)

    @mainargs.main(
        name = "configure",
        doc = "configure the JDBC connection to the legacy VARS database"
    )
    def configure(): Unit =
        Configure.run()

    @mainargs.main(
        name = "service-health",
        doc = "Check the health of the services"
    )
    def serviceHealth(): Unit =
        println("Checking services")
        if !serviceCheck() then println("Did you run `login` and `configure`? Go do that.")
        else
            given AnnotationService = serviceBuilder.annotationService
            given MediaService      = serviceBuilder.mediaService
            if (toolBeltOption.isEmpty) then
                println("No ToolBelt configured")
            ServiceHealth.run()

    @mainargs.main(
        name = "preview",
        doc = "Preview the migration of a all video archives"
    )
    def preview(): Unit =
        println("Running migration preview ...")
        if !serviceCheck() then println("Did you run `login` and `configure`? Go do that.")
        else
            given AnnotationService = serviceBuilder.annotationService
            given MediaService      = serviceBuilder.mediaService
            given MediaFactory      = mediaFactory
            given ToolBelt          = toolBeltOption.get
            Preview.run()

    @mainargs.main(
        name = "migrate-one",
        doc = "Migrate a single video archive"
    )
    def migrateOne(
        @arg(positional = true, doc = "The videoArchiveName to migrate") videoArchiveName: String
    ): Unit =
        given AnnotationService = serviceBuilder.annotationService
        given MediaService      = serviceBuilder.mediaService
        given MediaFactory      = mediaFactory
        given ToolBelt          = toolBeltOption.get
        MigrateOne.run(videoArchiveName)

    @mainargs.main(
        name = "migrate-all",
        doc = "Migrate a single video archive"
    )
    def migrateAll(): Unit =
        given AnnotationService = serviceBuilder.annotationService
        given MediaService      = serviceBuilder.mediaService
        given MediaFactory      = mediaFactory
        given ToolBelt          = toolBeltOption.get
        MigrateAll.run()

    @mainargs.main(
        name = "main-runner",
        doc = "A main app"
    )
    def run(
        @arg(positional = true, doc = "A message") msg: String
    ): Unit =
        log.log(Level.INFO, "1. Running with message: " + msg)
        log.atInfo.log("2. Running with message: " + msg)

    private def serviceCheck(): Boolean =
        if toolBeltOption.isDefined then Login.load().isDefined
        else {
            println("No ToolBelt configured")
            false
        }
