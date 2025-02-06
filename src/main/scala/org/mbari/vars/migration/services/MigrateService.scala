/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services

import org.mbari.vars.annosaurus.sdk.r1.{AnnosaurusHttpClient, AnnotationService}
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
        log.atInfo.log(s"---- Starting migration of ${videoArchive.getName} with missionContact $missionContact")
        if canMigrate(videoArchive) then
            findOrCreateMedia(videoArchive) match
                case Some(media) =>
                    migrateAnnotations(videoArchive, media, missionContact) // TODO - Implement this
                case None =>
                    log.atWarn.log(s"Not able to transform ${videoArchive.getName} to a media object")


    def canMigrate(videoArchive: VideoArchive): Boolean =
        val n = videoArchive.getVideoFrames.size()
        if n == 0 then
            log.atWarn.log(s"SKIPPING. No video frames found for ${videoArchive.getName}")
            false
        else
            mediaFactory.toMedia(videoArchive) match
                case Some(media) =>
                    // Check if URI already exists
                    val mediaOpt = vampireSquidService.findByUri(media.getUri)
                    mediaOpt match
                        case Some(existingMedia) =>
                            val gson = AnnosaurusHttpClient.newGson()
                            log.atWarn.log(gson.toJson(existingMedia))
                            log.atDebug.log(s"Media with URI ${existingMedia.getUri} already exists. Checking for annotations ... ")
                            // Check if there are any annotations for this media. If so, skip migration
                            val count = annotationService.countAnnotations(existingMedia.getVideoReferenceUuid).join()
                            if count.getCount > 0 then
                                log.atWarn.log(s"SKIPPING. Media with URI ${existingMedia.getUri} already has annotations.")
                                false
                            else
                                log.atDebug.log("No annotations found for this media. Ready to migrate.")
                                true
                        case None => true
                case None =>
                    log.atWarn.log(s"SKIPPING. Not able to transform ${videoArchive.getName} to a media object")
                    false

    private def findOrCreateMedia(videoArchive: VideoArchive): Option[Media] =
        val newMedia =  mediaFactory.toMedia(videoArchive).get // we're always calling canMigrate first. So this should always be Some
        val existingMediaOpt = vampireSquidService.findByUri(newMedia.getUri)
        existingMediaOpt match
            case Some(existingMedia) => Some(existingMedia)
            case None =>
                vampireSquidService.create(newMedia)

    def migrateAnnotations(videoArchive: VideoArchive, media: Media, group: String): Unit =
        // Convert observations to annotations
        // Create annotations
        val videoFrames = videoArchive.getVideoFrames.asScala
        val annotations = videoFrames.flatMap(vf => AnnotationFactory.toAnnotations(media, vf, group))
        val gson = AnnosaurusHttpClient.newGson()
        log.atInfo.log(s"Creating ${annotations.size} annotations for ${videoArchive.getName}")

        // TODO - Create annotations
        annotations.grouped(50)
            .foreach(annos =>
                val json = gson.toJson(annos.asJava)
                log.atInfo.log(s"SENDING:\n$json"))
//        annotations.grouped(50)
//            .foreach(annos => annotationService.createAnnotations(annos.asJava).join())

