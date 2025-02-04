/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import org.mbari.scommons.etc.jdk.Loggers.given
import org.mbari.vars.annosaurus.sdk.r1.AnnosaurusHttpClient
import org.mbari.vars.migration.AppConfig
import org.mbari.vars.migration.services.VarsLegacyService
import vars.ToolBelt

import java.time.Duration
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import java.nio.file.Path
import org.mbari.vars.migration.services.MigrateService

object MigrateOne:

    private val log               = System.getLogger(getClass.getName)
    private val toolBelt          = ToolBelt.defaultToolBelt()
    private val varsLegacyService = VarsLegacyService(toolBelt)
    private val annosaurusClient  =
        new AnnosaurusHttpClient(AppConfig.Annosaurus.Url, Duration.ofSeconds(20), AppConfig.Annosaurus.Secret)


    def run(videoArchiveName: String, cvsLookup: Path): Unit =
        println("Running MigrateOne")
        val migrateService = new MigrateService(toolBelt, cvsLookup)
        val dao = toolBelt.getAnnotationDAOFactory.newVideoArchiveDAO()
        val opt = varsLegacyService.findVideoArchiveSetByVideoArchiveName(videoArchiveName)
        opt match
            case None                  => log.atWarn.log(s"No VideoArchiveSet found for $videoArchiveName")
            case Some(videoArchiveSet) =>
                val missionContact = videoArchiveSet.getCameraDeployments.asScala.head.getChiefScientistName
                for videoArchive <- videoArchiveSet.getVideoArchives.asScala
                do
                    try
                        val frames = videoArchive.getVideoFrames().asScala
                        if frames.isEmpty then
                            log.atWarn.log(s"No video frames found for $videoArchiveName")
                        else
                        // do something
                            migrateService.migrate(videoArchive, missionContact)
                    catch
                        case NonFatal(e) =>
                            log.atError.withCause(e).log(s"Failed to migrate $videoArchiveName")
