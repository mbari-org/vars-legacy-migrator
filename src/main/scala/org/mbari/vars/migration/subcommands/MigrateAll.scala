/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import org.mbari.vars.annosaurus.sdk.r1.AnnotationService
import org.mbari.vars.migration.model.MediaFactory
import org.mbari.vars.migration.services.VarsLegacyService
import org.mbari.vars.vampiresquid.sdk.r1.MediaService
import vars.ToolBelt
import org.mbari.vars.migration.etc.jdk.Loggers.given
import scala.util.boundary
import scala.util.boundary.break

// Need to be able to provide a default ission contact

object MigrateAll:

    private val log = System.getLogger(getClass.getName)

    def run(group: String, limit: Int = -1)(using
        annotationService: AnnotationService,
        mediaService: MediaService,
        mediaFactory: MediaFactory,
        toolBelt: ToolBelt,
    ): Unit =
        val varsLegacyService = VarsLegacyService()
        val videoArchiveNames = varsLegacyService.findAllVideoArchiveNames()
        var count             = 0
        boundary {
            for videoArchiveName <- videoArchiveNames do
                if limit >= 0 && count >= limit then 
                    log.atInfo.log(s"Reached limit of $limit. Stopping migration.")
                    break()
                else
                    val ok = MigrateOne.run(videoArchiveName, group) 
                    if ok then count += 1
        }
