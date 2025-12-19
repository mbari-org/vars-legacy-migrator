/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import org.mbari.vars.annosaurus.sdk.r1.AnnotationService
import org.mbari.vars.migration.model.MediaFactory
import org.mbari.vars.migration.services.{MigrateService, VarsLegacyService}
import org.mbari.vars.migration.subcommands.MigrateOne.{getClass, log}
import org.mbari.vars.vampiresquid.sdk.r1.MediaService
import vars.ToolBelt
import org.mbari.vars.migration.etc.jdk.Loggers.given

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

object Preview:

    private val log = System.getLogger(getClass.getName)

    def run()(using
        annotationService: AnnotationService,
        mediaService: MediaService,
        mediaFactory: MediaFactory,
        toolBelt: ToolBelt
    ): Unit =
        given varsLegacyService: VarsLegacyService = VarsLegacyService()
        given migrateService: MigrateService       = MigrateService()
        val videoArchiveNames                      = varsLegacyService.findAllVideoArchiveNames()
        for videoArchiveName <- videoArchiveNames do preview(videoArchiveName)

    private def preview(videoArchiveName: String)(using
        annotationService: AnnotationService,
        mediaService: MediaService,
        mediaFactory: MediaFactory,
        toolBelt: ToolBelt,
        varsLegacyService: VarsLegacyService,
        migrateService: MigrateService
    ): Unit =
        val opt = varsLegacyService.findVideoArchiveSetByVideoArchiveName(videoArchiveName)
        opt match
            case None => log.atWarn.log(s"No VideoArchiveSet found for $videoArchiveName")
            case Some(videoArchiveSet) =>
                val missionContact = videoArchiveSet.getCameraDeployments.asScala.  head.getChiefScientistName
                for videoArchive <- videoArchiveSet.getVideoArchives.asScala do
                    if migrateService.canMigrate(videoArchive) then
                        val media = mediaFactory.toMedia(videoArchive)
                        media match
                            case Some(m) =>
                                val duration = m.getDuration.toMinutes
                                println(s"---- Preview migration of ${videoArchive.getName} ----")
                                println(s"Video Sequence Name: ${m.getVideoSequenceName}")
                                println(s"Video Name:          ${m.getVideoName}")
                                println(s"Video Reference URI: ${m.getUri}")
                                println(s"Video start:         ${m.getStartTimestamp}")
                                println(s"Video duration:      ${duration} minutes")

                            case None    =>
                                log.atWarn.log(s"Not able to transform ${videoArchive.getName} to a media object")

