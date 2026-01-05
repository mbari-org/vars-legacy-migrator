/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.model

import org.mbari.vars.migration.services.{
    BiauvTransform,
    GridPulseTransform,
    RoverChamberPulseTransform,
    RoverFluoroPulseTransform,
    RoverTransitPulseTransform,
    SesTransform,
    SimpaTransform,
    SpecialCasesTransform,
    TiburonCoolpixTransform,
    TripodPulseTransform,
    VideoArchiveTransform
}
import org.mbari.vars.vampiresquid.sdk.r1.models.Media
import vars.annotation.VideoArchive

import java.net.URL
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

class MediaFactory(csvLookup: URL):

    private val transforms: Seq[VideoArchiveTransform] = Seq(
        SpecialCasesTransform,
        BiauvTransform,
        SimpaTransform,
        GridPulseTransform,
        RoverChamberPulseTransform,
        RoverFluoroPulseTransform,
        RoverTransitPulseTransform,
        SesTransform,
        TiburonCoolpixTransform,
        TripodPulseTransform(csvLookup)
    )

    def toMedia(videoArchive: VideoArchive): Either[MediaTransformError, Media] =
        val vfs = if videoArchive.getVideoFrames == null then Nil else videoArchive.getVideoFrames.asScala

        if vfs.isEmpty then Left(NoVideoFramesError(videoArchive.getName))
        else
            val transformOpt = transforms.find(_.canTransform(videoArchive))
            transformOpt match
                case None            => Left(UnsupportedVideoArchiveError(videoArchive.getName))
                case Some(transform) =>
                    try
                        transform.transform(videoArchive) match
                            case Some(media) => Right(media)
                            case None        => Left(UnsupportedVideoArchiveError(videoArchive.getName))
                    catch case NonFatal(e) => Left(TransformationFailedError(videoArchive.getName, e))

object MediaFactory:

    def load(): MediaFactory =
        val stationM = getClass.getResource("/VARS_IMAGES_dbo_VideoArchive_edited.csv")
        new MediaFactory(stationM)
