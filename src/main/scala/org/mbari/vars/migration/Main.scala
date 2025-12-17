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
import org.mbari.vars.migration.subcommands.{MigrateAll, MigrateOne, ServiceHealth}
import org.mbari.vars.vampiresquid.sdk.r1.MediaService
import vars.ToolBelt

import java.lang.System.Logger.Level
import java.nio.file.Path

object Main:

    // Needed for mainargs to parse Path arguments
    given TokensReader.Simple[Path] = PathReader
    given AnnotationService         = AppConfig.Annosaurus.defaultService
    given MediaService              = AppConfig.VampireSquid.defaultService
    given ToolBelt                  = AppConfig.VarsLegacy.defaultToolBelt

    private val log = System.getLogger(Main.getClass.getName)

    def main(args: Array[String]): Unit =
        ParserForMethods(this).runOrExit(args.toSeq)
        System.exit(0)

    @mainargs.main(
        name = "service-health",
        doc = "Check the health of the services"
    )
    def serviceHealth(): Unit =
        log.atInfo.log("Checking services")
        ServiceHealth.run()

    @mainargs.main(
        name = "migrate-one",
        doc = "Migrate a single video archive"
    )
    def migrateOne(
        @arg(positional = true, doc = "The videoArchiveName to migrate") videoArchiveName: String,
        @arg(positional = true, doc = "Path to CSV lookup file") csvLookup: Path
    ): Unit =
        log.atInfo.log(s"Running MigrateOne using CSV lookup file: $csvLookup with videoArchiveName: $videoArchiveName")
        given MediaFactory = MediaFactory(csvLookup)
        MigrateOne.run(videoArchiveName)

    @mainargs.main(
        name = "migrate-all",
        doc = "Migrate a single video archive"
    )
    def migrateAll(@arg(positional = true, doc = "Path to CSV lookup file") csvLookup: Path): Unit =
        log.atInfo.log(s"Running MigrateAll using CSV lookup file: $csvLookup")
        given MediaFactory = MediaFactory(csvLookup)
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
