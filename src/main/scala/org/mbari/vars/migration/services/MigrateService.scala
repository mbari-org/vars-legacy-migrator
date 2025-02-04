/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services

import org.mbari.vars.migration.model.MediaFactory
import vars.ToolBelt
import vars.annotation.VideoArchive

import java.nio.file.Path

class MigrateService(toolBelt: ToolBelt, csvLookup: Path):

    private val mediaFactory = new MediaFactory(csvLookup)

    def migrate(videoArchive: VideoArchive, missionContact: String): Unit =
        println(s"Migrating ${videoArchive.getName} with missionContact $missionContact")
        // Do the migration here
        mediaFactory.toMedia(videoArchive) match
            case Some(media) =>
                println(s"Media: ${media.getUri}, ${media.getVideoSequenceName}, ${media.getVideoName}")
                println(s"MissionContact: $missionContact")
                println(s"VideoArchive: ${videoArchive.getName} with ${videoArchive.getVideoFrames.size()} video frames")
            case None =>
                println(s"Not able to transform ${videoArchive.getName} to a media object")
        println("Migration complete")
        println("")
