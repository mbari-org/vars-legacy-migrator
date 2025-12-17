/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import org.mbari.scommons.etc.jdk.Loggers.given
import org.mbari.vars.annosaurus.sdk.r1.AnnotationService
import org.mbari.vars.vampiresquid.sdk.r1.MediaService

import scala.util.control.NonFatal

object ServiceHealth:

    private val log = System.getLogger(getClass.getName)

    def run()(using annotationService: AnnotationService, mediaService: MediaService): Unit =

        try
            annotationService.findActivities().join()
            log.atInfo.log("Connected to Annosaurus")
        catch
            case NonFatal(e) =>
                log.atError.withCause(e).log("Failed to connect to Annosaurus")

        try
            mediaService.findAllCameraIds().join()
            log.atInfo.log("Connected to VampireSquid")
        catch
            case NonFatal(e) =>
                log.atError.withCause(e).log("Failed to connect to VampireSquid")
