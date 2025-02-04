/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration

import mainargs.{arg, main, ParserForMethods, TokensReader}
import org.mbari.scommons.etc.jdk.Loggers.given
import org.mbari.vars.migration.etc.mainargs.PathReader
import org.mbari.vars.migration.subcommands.MigrateOne

import java.lang.System.Logger.Level
import java.nio.file.Path

object Main:

    // Needed for mainargs to parse Path arguments
    given TokensReader.Simple[Path] = PathReader

    private val log = System.getLogger(Main.getClass.getName)

    def main(args: Array[String]): Unit =
        ParserForMethods(this).runOrExit(args.toSeq)
        System.exit(0)


    @mainargs.main(
        name = "migrate-one",
        doc = "Migrate a single video archive"
    )
    def migrateOne(
              @arg(positional = true, doc = "The videoArchiveName to migrate") videoArchiveName: String,
              @arg(positional = true, doc = "Path to CSV lookup file") csvLookup: Path): Unit =
        log.atInfo.log("1. Running MigrateOne with videoArchiveName: " + videoArchiveName)
        MigrateOne.run(videoArchiveName, csvLookup)

    @mainargs.main(
        name = "main-runner",
        doc = "A main app"
    )
    def run(
        @arg(positional = true, doc = "A message") msg: String
    ): Unit =
        log.log(Level.INFO, "1. Running with message: " + msg)
        log.atInfo.log("2. Running with message: " + msg)
