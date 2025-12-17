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

// Need to be able to provide a default ission contact

object MigrateAll:

    private val log = System.getLogger(getClass.getName)

    def run()(using
        annotationService: AnnotationService,
        mediaService: MediaService,
        mediaFactory: MediaFactory,
        toolBelt: ToolBelt
    ): Unit =
        val varsLegacyService = VarsLegacyService()
        val videoArchiveNames = varsLegacyService.findAllVideoArchiveNames()
        for videoArchiveName <- videoArchiveNames do MigrateOne.run(videoArchiveName)
