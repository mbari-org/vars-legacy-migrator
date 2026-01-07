/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import org.mbari.scommons.etc.jdk.Loggers.given
import org.mbari.vars.annosaurus.sdk.r1.AnnotationService
import org.mbari.vars.migration.model.MediaFactory
import org.mbari.vars.migration.services.{MigrateService, VarsLegacyService}
import org.mbari.vars.vampiresquid.sdk.r1.MediaService
import vars.ToolBelt

import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

object MigrateOne:

    private val log = System.getLogger(getClass.getName)

    def run(videoArchiveName: String, group: String)(using
        annotationService: AnnotationService,
        mediaService: MediaService,
        mediaFactory: MediaFactory,
        toolBelt: ToolBelt
    ): Boolean =
        val migrateService    = MigrateService()
        val varsLegacyService = VarsLegacyService()
        val opt               = varsLegacyService.findVideoArchiveSetByVideoArchiveName(videoArchiveName)
        opt match
            case None                  => 
                log.atWarn.log(s"No VideoArchiveSet found for $videoArchiveName")
                false
            case Some(videoArchiveSet) =>
                val opt = videoArchiveSet.getVideoArchives().asScala.find(va => va.getName == videoArchiveName)
                opt match
                    case None  =>
                        log.atWarn.log(s"VideoArchive '$videoArchiveName' not found in VideoArchiveSet '${videoArchiveSet}'")
                        false
                    case Some(videoArchive) =>
                        var success = false
                        try
                            val frames = videoArchive.getVideoFrames().asScala
                            if frames.isEmpty then 
                                log.atWarn.log(s"No video frames found for $videoArchiveName")
                            else
                                // do something
                                success = migrateService.migrate(videoArchive, group) 
                        catch
                            case NonFatal(e) =>
                                log.atError.withCause(e).log(s"Failed to migrate $videoArchiveName")
                        success 
                

