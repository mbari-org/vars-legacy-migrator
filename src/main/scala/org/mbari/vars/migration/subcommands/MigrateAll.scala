/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import org.mbari.vars.annosaurus.sdk.r1.{AnnosaurusHttpClient, AnnotationService}
import org.mbari.vars.migration.AppConfig
import org.mbari.vars.migration.services.VarsLegacyService
import org.mbari.vars.migration.subcommands.MigrateOne.getClass
import vars.ToolBelt
import org.mbari.scommons.etc.jdk.Futures.given
import org.mbari.vars.migration.model.MediaFactory
import org.mbari.vars.vampiresquid.sdk.r1.MediaService

import java.nio.file.Path
import java.time.Duration
import scala.jdk.CollectionConverters.*

// Need to be able to provide a default ission contact

object MigrateAll:

    private val log = System.getLogger(getClass.getName)

    def migrate()(using annotationService: AnnotationService,
                                 mediaService: MediaService,
                                 mediaFactory: MediaFactory,
                                 toolBelt: ToolBelt): Unit =
        val varsLegacyService = VarsLegacyService()
        val videoArchiveNames = varsLegacyService.findAllVideoArchiveNames()



