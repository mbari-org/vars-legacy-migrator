/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.model

abstract class MediaTransformError(cause: Throwable = null) extends Exception(cause):
    def videoArchiveName: String

case class NoVideoFramesError(videoArchiveName: String) extends MediaTransformError:
    override def getMessage: String = s"VideoArchive '$videoArchiveName' has no video frames."

case class UnsupportedVideoArchiveError(videoArchiveName: String) extends MediaTransformError:
    override def getMessage: String = s"VideoArchive '$videoArchiveName' is of an unsupported type for transformation."

case class TransformationFailedError(videoArchiveName: String, cause: Throwable) extends MediaTransformError:
    override def getMessage: String =
        s"Transformation of VideoArchive '$videoArchiveName' failed: ${cause.getMessage()}"

case class CantMigrateError(videoArchiveName: String, reason: String) extends MediaTransformError:
    override def getMessage: String = s"Cannot migrate VideoArchive '$videoArchiveName': $reason"
