/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services

import org.mbari.vars.vampiresquid.sdk.r1.models.Media
import vars.annotation.VideoArchive

import java.net.{URI, URL}

import java.time.Instant
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.matching.Regex
import org.mbari.vars.migration.etc.vampiresquid.org.mbari.vars.vampiresquid.sdk.r1.models.MediaBuilder

def extractTwoDigitNumber(s: String): Option[String] =
    val regex: Regex = """\b\d{2}\b""".r
    regex.findFirstIn(s)

def extractDashNumber(s: String): Option[String] =
    val regex: Regex = """-(\d+)""".r
    regex.findFirstMatchIn(s).map(_.group(1))

def findStartTimestamp(videoArchive: VideoArchive): Option[Instant] =
    val vfs = if videoArchive.getVideoFrames == null then Nil else videoArchive.getVideoFrames.asScala
    vfs
        .filter(f => f.getRecordedDate != null)
        .map(f => f.getRecordedDate.toInstant)
        .sorted
        .headOption

trait VideoArchiveTransform:

    val UriPrefix = "urn:imagecollection:org.mbari:"

    def canTransform(videoArchive: VideoArchive): Boolean
    def transform(videoArchive: VideoArchive): Option[Media]
    def cameraId: String

trait StationMVideoArchiveTransform extends VideoArchiveTransform:

    def toVideoSequenceName(videoArchive: VideoArchive): Option[String] =
        val deployment = extractTwoDigitNumber(videoArchive.getName)
        deployment.map(d => s"Station M Pulse $d - $cameraId")

    def toUri(videoArchive: VideoArchive): Option[URI] =
        toVideoSequenceName(videoArchive).map(videoSequenceName =>
            val uriString = s"$UriPrefix${videoSequenceName.replace(" ", "_").replace("-", "")}"
            URI.create(uriString)
        )

trait StationMTransform extends StationMVideoArchiveTransform:

    override def transform(videoArchive: VideoArchive): Option[Media] =
        for
            deployment        <- extractTwoDigitNumber(videoArchive.getName)
            videoSequenceName <- toVideoSequenceName(videoArchive)
            uri               <- toUri(videoArchive)
        yield
            val media = Media()
            media.setVideoSequenceName(videoSequenceName)
            media.setVideoName(videoSequenceName)
            media.setCameraId(cameraId)
            media.setUri(uri)
            findStartTimestamp(videoArchive).foreach(media.setStartTimestamp)
            media.setVideoDescription(
                s"Station M $cameraId deployment $deployment imported from VARS_Images.  Original VideoArchive name: ${videoArchive.getName}"
            )
            media

/**
 * Transform for Grid Pulse video archives
 */
object GridPulseTransform extends StationMVideoArchiveTransform:

    override def cameraId: String = "Tripod Mixed Far Field"

    override def canTransform(videoArchive: VideoArchive): Boolean =
        videoArchive.getName.startsWith("Grid Pulse")

    override def transform(videoArchive: VideoArchive): Option[Media] =
        val deployment = extractTwoDigitNumber(videoArchive.getName)
        for
            deployment        <- extractTwoDigitNumber(videoArchive.getName)
            videoSequenceName <- toVideoSequenceName(videoArchive)
        yield
            val vsn       = videoSequenceName + " - Grid Overlay"
            val uriString = s"$UriPrefix${vsn.replace(" ", "_").replace("-", "")}"
            val uri       = URI.create(uriString)
            val media     = Media()
            media.setVideoSequenceName(vsn)
            media.setVideoName(vsn)
            media.setCameraId(cameraId)
            media.setUri(uri)
            findStartTimestamp(videoArchive).foreach(media.setStartTimestamp)
            media.setVideoDescription(
                s"Station M Pulse $deployment tripod images imported from VARS_Images. These images have a visual grid overlay. Original VideoArchive name: ${videoArchive.getName}"
            )
            media

object MacroCamTransform extends VideoArchiveTransform:

    override def cameraId: String = "Macro Cam"

    override def canTransform(videoArchive: VideoArchive): Boolean =
        videoArchive.getName.startsWith("MacroCam")

    override def transform(videoArchive: VideoArchive): Option[Media] =
        val videoSequenceName = "Macro Cam 202109" // HACK - there's only one MacroCam video archive
        val videoName         = videoSequenceName
        val uriString         = s"$UriPrefix${videoSequenceName.replace(" ", "_")}"
        val media             = Media()
        media.setVideoSequenceName(videoSequenceName)
        media.setVideoName(videoName)
        media.setCameraId(cameraId)
        media.setUri(URI.create(uriString))
        findStartTimestamp(videoArchive).foreach(media.setStartTimestamp)
        media.setVideoDescription(
            s"MacroCam imported from VARS_Images.  Original VideoArchive name: ${videoArchive.getName}"
        )
        Some(media)

object RoverChamberPulseTransform extends StationMTransform:

    override def cameraId: String = "Benthic Rover Respiration Chamber Camera"

    override def canTransform(videoArchive: VideoArchive): Boolean =
        videoArchive.getName.startsWith("Rover Chamber Pulse")

object RoverFluoroPulseTransform extends StationMTransform:

    override def cameraId: String = "Benthic Rover Fluoro Camera"

    override def canTransform(videoArchive: VideoArchive): Boolean =
        videoArchive.getName.toLowerCase.startsWith("rover fluoro pulse")

object RoverTransitPulseTransform extends StationMTransform:

    override def cameraId: String = "Benthic Rover Transit Camera"

    override def canTransform(videoArchive: VideoArchive): Boolean =
        videoArchive
            .getName
            .toLowerCase
            .replace("_", " ") // How should I handle "Rover_transit_Pulse_58_Ken" vs "Rover Transit Pulse 58"
            .startsWith("rover transit pulse")

object SesTransform extends StationMTransform:

    override def cameraId: String = "Sedimentation Event Sensor"

    override def canTransform(videoArchive: VideoArchive): Boolean =
        videoArchive.getName.toLowerCase.startsWith("ses pulse")

object TiburonCoolpixTransform extends VideoArchiveTransform:

    override def cameraId: String = "Tiburon Coolpix"

    override def canTransform(videoArchive: VideoArchive): Boolean =
        val videoArchiveName = videoArchive.getName.toUpperCase()
        videoArchiveName.startsWith("T") && videoArchiveName.contains("COOLPIX")

    override def transform(videoArchive: VideoArchive): Option[Media] =
        val sequenceNumber    = videoArchive
            .getVideoArchiveSet
            .getCameraDeployments
            .asScala
            .head
            .getSequenceNumber
            .intValue
        val videoSequenceName = s"Tiburon $sequenceNumber - Coolpix"
        val videoName         = videoSequenceName
        val uriString         = s"$UriPrefix${videoSequenceName.replace(" ", "_").replace("-", "")}"
        val media             = Media()
        media.setVideoSequenceName(videoSequenceName)
        media.setVideoName(videoName)
        media.setCameraId(cameraId)
        media.setUri(URI.create(uriString))
        findStartTimestamp(videoArchive).foreach(media.setStartTimestamp)
        media.setVideoDescription(
            s"Tiburon Coolpix imported from VARS_Images.  Original VideoArchive name: ${videoArchive.getName}"
        )
        Some(media)

class TripodPulseTransform(csvLookup: URL) extends VideoArchiveTransform:

    // Read CSV file , first column is videoArchive name, second column is cameraId
    val cameraIdMap: Map[String, String] =
        Using(Source.fromURL(csvLookup, "UTF-8")) { source =>
            source
                .getLines()
                .map { line =>
                    val Array(videoArchiveName, cameraId) = line.split(",")
                    videoArchiveName -> cameraId
                }
                .toMap
        }.getOrElse(Map.empty)

    override def cameraId: String = ???

    override def canTransform(videoArchive: VideoArchive): Boolean =
        videoArchive.getName.toLowerCase.startsWith("tripod pulse")

    override def transform(videoArchive: VideoArchive): Option[Media] =
        for deployment <- extractTwoDigitNumber(videoArchive.getName)
        yield
            val cameraId          = extractCameraId(videoArchive.getName)
            val dashNumber        = extractDashNumber(videoArchive.getName).getOrElse("")
            val videoSequenceName = s"Station M Pulse $deployment - $cameraId"
            val videoArchiveName  = s"Tripod Pulse $deployment$dashNumber - $cameraId"
            val uriString         = s"$UriPrefix${videoSequenceName.replace(" ", "_").replace("-", "")}"
            val uri               = URI.create(uriString)
            val media             = Media()
            media.setVideoSequenceName(videoSequenceName)
            media.setVideoName(videoArchiveName)
            media.setCameraId(cameraId)
            media.setUri(uri)
            findStartTimestamp(videoArchive).foreach(media.setStartTimestamp)
            media.setVideoDescription(
                s"Station M $cameraId deployment $deployment imported from VARS_Images.  Original VideoArchive name: ${videoArchive.getName}"
            )
            media

    def extractCameraId(videoArchiveName: String): String =
        val upper = videoArchiveName.toUpperCase()
        if upper.startsWith("TRIPOD PULSE") then
            if upper.contains("NEA") then "Tripod Near Field"
            else if upper.contains("DUAL") then "Tripod Far Field M"
            else if upper.contains("SOLO") then "Tripod Far Field S"
            else "Tripod Far Field M"
        else "Unknown"


object SpecialCasesTransform extends VideoArchiveTransform:

    private val specialCases: Map[String, Media] = Map(
        // VideoArchive name -> CameraId
        "PAP-JC062-119" -> MediaBuilder() // TODO
            .withVideoSequenceName("Porcupine Abyssal Plain - Time Lapse JC062-119")
            .withVideoName("PAP JC062-119 Time Lapse")
            .withCameraId("PAP Time Lapse Camera")
            .withUri(URI.create("urn:imagecollection:org.mbari:Porcupine_Abyssal_Plain___Time_Lapse_JC062_119"))
            .withVideoDescription("PAP-JC062-119 imported from VARS_Images.")
            .build(),
        "PAP-JC071-043" -> MediaBuilder() // TODO
            .withVideoSequenceName("Porcupine Abyssal Plain - Time Lapse JC071-043")
            .withVideoName("PAP JC071-043 Time Lapse")
            .withCameraId("PAP Time Lapse Camera")
            .withUri(URI.create("urn:imagecollection:org.mbari:Porcupine_Abyssal_Plain___Time_Lapse_JC071_043"))
            .withVideoDescription("PAP-JC071-043 imported from VARS_Images.")
            .build(),
        "Rover_transit_Pulse_58_Ken" -> MediaBuilder() // TODO
            .build(),
        "Rover_transit_Pulse_59_Ken" -> MediaBuilder() // TODO
            .build(),
        "Rover_transit_Pulse_60_Ken" -> MediaBuilder() // TODO
            .build(),
        "Rover_transit_Pulse_71" -> MediaBuilder() // TODO
            .build(),
        "Rover_transit_Pulse_72" -> MediaBuilder() // TODO
            .build(),
        "S0055-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 55 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 55-S1 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_55-S1___Tripod_Far_Field_M"))
            .withVideoDescription("S0055-01-tripod imported from VARS_Images.")
            .build(),
        "S6510-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field S")
            .withVideoSequenceName("Station M Pulse 65 - Tripod Far Field S")
            .withVideoName("Tripod Pulse 65-S1 - Tripod Far Field S")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_65-S1___Tripod_Far_Field_S"))
            .withVideoDescription("S6510-01-tripod imported from VARS_Images.")
            .build(),
        "T0510-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field S")
            .withVideoSequenceName("Station M Pulse 05 - Tripod Far Field S")
            .withVideoName("Tripod Pulse 05-S1 - Tripod Far Field S")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_05-S1___Tripod_Far_Field_S"))
            .withVideoDescription("T0510-01-tripod imported from VARS_Images.")
            .build(),
        "T0715-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 07 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 07-S1 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_07-S1___Tripod_Far_Field_M"))
            .withVideoDescription("T0715-01-tripod imported from VARS_Images.")
            .build(),
        "T17021-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 17 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 17-S1 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_17-S1___Tripod_Far_Field_M"))
            .withVideoDescription("T17021-01-tripod imported from VARS_Images.")
            .build(),
        "T2231-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 22 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 22-S1 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_22-S1___Tripod_Far_Field_M"))
            .withVideoDescription("T2231-01-tripod imported from VARS_Images.")
            .build(),
        "T22311-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 22 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 22-S11 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_22-S11___Tripod_Far_Field_M"))
            .withVideoDescription("T22311-01-tripod imported from VARS_Images.")
            .build(),
        "T26121-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 26 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 26-S1 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_26-S1___Tripod_Far_Field_M"))
            .withVideoDescription("T26121-01-tripod imported from VARS_Images.")
            .build(),
        "T7021-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 07 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 07-S1 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_07-S1___Tripod_Far_Field_M"))
            .withVideoDescription("T7021-01-tripod imported from VARS_Images.")
            .build(),
        "T70211-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 07 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 07-S11 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_07-S11___Tripod_Far_Field_M"))
            .withVideoDescription("T70211-01-tripod imported from VARS_Images.")
            .build(),
        "T70212011-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 07 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 07-S1201 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_07-S1201___Tripod_Far_Field_M"))
            .withVideoDescription("T70212011-01-tripod imported from VARS_Images.")
            .build(),
        "T7022-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 07 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 07-S2 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_07-S2___Tripod_Far_Field_M"))
            .withVideoDescription("T7022-01-tripod imported from VARS_Images.")
            .build(),
        "T70221-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 07 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 07-S21 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_07-S21___Tripod_Far_Field_M"))
            .withVideoDescription("T70221-01-tripod imported from VARS_Images.")
            .build(),
        "T7023-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 07 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 07-S3 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_07-S3___Tripod_Far_Field_M"))
            .withVideoDescription("T7023-01-tripod imported from VARS_Images.")
            .build(),
        "T70231-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 07 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 07-S31 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_07-S31___Tripod_Far_Field_M"))
            .withVideoDescription("T70231-01-tripod imported from VARS_Images.")
            .build(),
        "T7024-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 07 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 07-S4 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_07-S4___Tripod_Far_Field_M"))
            .withVideoDescription("T7024-01-tripod imported from VARS_Images.")
            .build(),
        "T70241-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 07 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 07-S41 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_07-S41___Tripod_Far_Field_M"))
            .withVideoDescription("T70241-01-tripod imported from VARS_Images.")
            .build(),
        "T7107-01-tripod" -> MediaBuilder()
            .withCameraId("Tripod Far Field Solo")
            .withVideoSequenceName("Station M Pulse 71 - Tripod Far Field S")
            .withVideoName("Tripod Pulse 71-S1 - Tripod Far Field S")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_71-S1___Tripod_Far_Field_S"))
            .withVideoDescription("T7107-01-tripod imported from VARS_Images.")
            .build()
    )

    override def cameraId: String = ???

    override def canTransform(videoArchive: VideoArchive): Boolean =
        specialCases.contains(videoArchive.getName)

    override def transform(videoArchive: VideoArchive): Option[Media] =
        specialCases.get(videoArchive.getName)