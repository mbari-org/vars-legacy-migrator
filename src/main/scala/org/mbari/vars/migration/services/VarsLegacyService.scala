package org.mbari.vars.migration.services

import vars.ToolBelt
import vars.annotation.VideoArchiveSet
import scala.jdk.CollectionConverters.*

class VarsLegacyService(toolBelt: ToolBelt):

    def findVideoArchiveSetByVideoArchiveName(videoArchiveName: String): Option[VideoArchiveSet] =
        val dao = toolBelt.getAnnotationDAOFactory.newVideoArchiveDAO()
        dao.startTransaction()
        val opt = Option(dao.findByName(videoArchiveName)).map(_.getVideoArchiveSet)
        opt.map(_.getVideoFrames) // load the video frames in transaction
        dao.endTransaction()
        dao.close()
        opt

