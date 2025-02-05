/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services

import org.mbari.vars.annosaurus.sdk.r1.AnnotationService
import org.mbari.vars.migration.AppConfig
import org.mbari.vars.migration.model.{AnnotationFactory, MediaFactory}
import org.mbari.vars.vampiresquid.sdk.r1.{MediaService, VampireSquidKiotaClient}
import vars.ToolBelt
import vars.annotation.VideoArchive
import org.mbari.scommons.etc.jdk.Loggers.given
import org.mbari.vars.vampiresquid.sdk.r1.models.Media
import scala.jdk.CollectionConverters.*

import java.net.URI
import java.nio.file.Path
import scala.util.Try

class MigrateService(using annotationService: AnnotationService,
                     mediaService: MediaService,
                     mediaFactory: MediaFactory,
                     toolBelt: ToolBelt):

    private val log = System.getLogger(getClass.getName)
    private val vampireSquidService = VampireSquidService()



    def migrate(videoArchive: VideoArchive, missionContact: String): Unit =
        println(s"Migrating ${videoArchive.getName} with missionContact $missionContact")
        // Do the migration here
        mediaFactory.toMedia(videoArchive) match
            case Some(media) =>
                println(s"Media: ${media.getUri}, ${media.getVideoSequenceName}, ${media.getVideoName}")
                println(s"MissionContact: $missionContact")
                println(s"VideoArchive: ${videoArchive.getName} with ${videoArchive.getVideoFrames.size()} video frames")

                // Check if URI already exists
                val mediaOpt = vampireSquidService.findByUri(media.getUri)
                mediaOpt match
                    case Some(existingMedia) =>
                        log.atInfo.log(s"Media with URI ${media.getUri} already exists. Skipping migration")
                    case None => // Do nothing
                        vampireSquidService.create(media) match
                            case None => // Do nothing
                            case Some(newMedia) =>
                                // TODO - Migrate annotations



            case None =>
                println(s"Not able to transform ${videoArchive.getName} to a media object")
        println("Migration complete")
        println("")

    def migrateAnnotations(videoArchive: VideoArchive, media: Media, group: String): Unit =
        // Convert observations to annotations
        // Create annotations
        val videoFrames = videoArchive.getVideoFrames.asScala
        val annotations = videoFrames.flatMap(vf => AnnotationFactory.toAnnotations(media, vf, group))

        // TODO - Create annotations

