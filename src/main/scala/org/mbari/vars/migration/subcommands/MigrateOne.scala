/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import org.mbari.vars.annosaurus.sdk.r1.AnnosaurusHttpClient
import org.mbari.vars.migration.AppConfig
import org.mbari.vars.migration.services.VarsLegacyService
import vars.ToolBelt
import org.mbari.scommons.etc.jdk.Loggers.given

import java.time.Duration
import scala.jdk.CollectionConverters.*


object MigrateOne:

    private val log = System.getLogger(getClass.getName)
    private val toolBelt = ToolBelt.defaultToolBelt()
    private val varsLegacyService = VarsLegacyService(toolBelt)
    private val annosaurusClient = new AnnosaurusHttpClient(
        AppConfig.Annosaurus.Url,
        Duration.ofSeconds(20),
        AppConfig.Annosaurus.Secret)

    def run(videoArchiveName: String): Unit =
        println("Running MigrateOne")
        val dao = toolBelt.getAnnotationDAOFactory.newVideoArchiveDAO()
        val opt = varsLegacyService.findVideoArchiveSetByVideoArchiveName(videoArchiveName)
        opt match
            case None => log.atWarn.log(s"No VideoArchiveSet found for $videoArchiveName")
            case Some(videoArchiveSet) =>
                val missionContact = videoArchiveSet.getCameraDeployments.asScala.head.getChiefScientistName



