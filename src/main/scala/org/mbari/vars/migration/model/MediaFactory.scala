/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.model

import org.mbari.vars.migration.services.{
    GridPulseTransform,
    MacroCamTransform,
    RoverChamberPulseTransform,
    RoverFluoroPulseTransform,
    RoverTransitPulseTransform,
    SesTransform,
    TiburonCoolpixTransform,
    TripodPulseTransform,
    VideoArchiveTransform
}
import org.mbari.vars.vampiresquid.sdk.r1.models.Media
import vars.annotation.VideoArchive

import java.nio.file.Path
import scala.jdk.CollectionConverters.*

class MediaFactory(csvLookup: Path):

    private val transforms: Seq[VideoArchiveTransform] = Seq(
        GridPulseTransform,
        MacroCamTransform,
        RoverChamberPulseTransform,
        RoverFluoroPulseTransform,
        RoverTransitPulseTransform,
        SesTransform,
        TiburonCoolpixTransform,
        TripodPulseTransform(csvLookup)
    )

    def toMedia(videoArchive: VideoArchive): Option[Media] =
        val vfs = if videoArchive.getVideoFrames == null then Nil else videoArchive.getVideoFrames.asScala

        if vfs.isEmpty then None
        else
            for
                transform <- transforms.find(_.canTransform(videoArchive))
                media     <- transform.transform(videoArchive)
            yield media
            
            
object MediaFactory:
    
    def load(): MediaFactory =
        val stationM = getClass.getResource("/VARS_IMAGES_dbo_VideoArchive_edited.csv")
        new MediaFactory(Path.of(stationM.toURI))
