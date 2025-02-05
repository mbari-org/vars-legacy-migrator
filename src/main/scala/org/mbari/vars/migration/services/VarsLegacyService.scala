/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services

import vars.ToolBelt
import vars.annotation.VideoArchiveSet
import scala.jdk.CollectionConverters.*

class VarsLegacyService(using toolBelt: ToolBelt):

    def findVideoArchiveSetByVideoArchiveName(videoArchiveName: String): Option[VideoArchiveSet] =
        val dao = toolBelt.getAnnotationDAOFactory.newVideoArchiveDAO()
        dao.startTransaction()
        val opt = Option(dao.findByName(videoArchiveName)).map(_.getVideoArchiveSet)
        opt.map(_.getVideoFrames) // load the video frames in transaction
        dao.endTransaction()
        dao.close()
        opt

    def findAllVideoArchiveNames(): Seq[String] =
        toolBelt.getAnnotationPersistenceService
            .findAllVideoArchiveNames()
            .asScala
            .toSeq
            .sorted
