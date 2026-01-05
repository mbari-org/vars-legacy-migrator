/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import org.mbari.vars.annosaurus.sdk.r1.AnnotationService
import org.mbari.vars.migration.etc.jdk.Durations
import org.mbari.vars.migration.model.{CantMigrateError, MediaFactory, MediaTransformError, NoVideoFramesError}
import org.mbari.vars.migration.services.{MigrateService, VarsLegacyService}
import org.mbari.vars.vampiresquid.sdk.r1.MediaService
import org.mbari.vars.vampiresquid.sdk.r1.models.Media
import vars.ToolBelt
import vars.annotation.VideoArchive

import scala.jdk.CollectionConverters.*
import scala.util.Try

object Preview:

    def run()(using
        annotationService: AnnotationService,
        mediaService: MediaService,
        mediaFactory: MediaFactory,
        toolBelt: ToolBelt
    ): Unit =
        given varsLegacyService: VarsLegacyService = VarsLegacyService()
        given migrateService: MigrateService       = MigrateService()
        val videoArchiveNames                      = varsLegacyService.findAllVideoArchiveNames()
        val columns                                = List(
            "videoArchiveName",
            "annotationCount",
            "videoSequenceName",
            "videoName",
            "cameraId",
            "uri",
            "startTimestamp",
            "duration",
            "notes"
        )
        println(columns.mkString(",")) // print header
        for videoArchiveName <- videoArchiveNames do preview(videoArchiveName)

    private def preview(videoArchiveName: String)(using
        annotationService: AnnotationService,
        mediaService: MediaService,
        mediaFactory: MediaFactory,
        toolBelt: ToolBelt,
        varsLegacyService: VarsLegacyService,
        migrateService: MigrateService
    ): Unit =
        // println(s"Previewing VideoArchive: $videoArchiveName")
        val opt = varsLegacyService.findVideoArchiveSetByVideoArchiveName(videoArchiveName)
        opt match
            case None                  =>
                println(noMatchMsg(videoArchiveName))
            case Some(videoArchiveSet) =>
                val missionContact = videoArchiveSet.getCameraDeployments.asScala.head.getChiefScientistName
                val videoArchive   = videoArchiveSet.getVideoArchives.asScala.find(_.getName == videoArchiveName)
                videoArchive match
                    case None               =>
                        println(noMatchMsg(videoArchiveName))
                    case Some(videoArchive) =>
                        val annotationCount = Try(videoArchive.getVideoFrames.size()).getOrElse(0)
                        if migrateService.canMigrate(videoArchive) then
                            val media = mediaFactory.toMedia(videoArchive)
                            media match
                                case Right(m) =>
                                    println(mediaMsg(videoArchive, m))
                                case Left(ex) =>
                                    println(errorMsg(ex, annotationCount))
                        else if annotationCount == 0 then
                            val ex = NoVideoFramesError(videoArchive.getName)
                            println(errorMsg(ex, 0))
                        else if annotationCount > 0 then
                            val ex = CantMigrateError(
                                videoArchive.getName,
                                s"Media with existing annotations in target database or unable to transform as mapping for ${videoArchive.getName} is missing"
                            )
                            println(errorMsg(ex, annotationCount))
                        else
                            val ex = CantMigrateError(videoArchive.getName, s"Unknown reason")
                            println(errorMsg(ex, annotationCount))

    private def noMatchMsg(videoArchiveName: String): String =
        List(
            videoArchiveName,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            s"No video archive named $videoArchiveName was found in the source database"
        ).mkString(",")

    private def mediaMsg(videoArchive: VideoArchive, media: Media): String =
        List(
            videoArchive.getName(),
            Try(videoArchive.getVideoFrames().size().toString()).getOrElse(0),
            media.getVideoSequenceName(),
            media.getVideoName(),
            media.getCameraId(),
            media.getUri(),
            Try(media.getStartTimestamp().toString()).getOrElse(""),
            Option(media.getDuration()).map(d => Durations.formatDuration(d)).getOrElse(""),
            ""
        ).mkString(",")

    private def errorMsg(error: MediaTransformError, annotationCount: Int): String =
        List(
            error.videoArchiveName,
            annotationCount.toString(),
            "",
            "",
            "",
            "",
            "",
            "",
            error.getMessage()
        ).mkString(",")
