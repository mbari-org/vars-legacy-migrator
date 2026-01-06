/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services

import org.mbari.vars.migration.etc.jdk.Instants
import org.mbari.vars.migration.etc.vampiresquid.org.mbari.vars.vampiresquid.sdk.r1.models.MediaBuilder
import org.mbari.vars.vampiresquid.sdk.r1.models.Media
import vars.annotation.VideoArchive

import java.net.{URI, URL}
import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, ZoneOffset, ZonedDateTime}
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.matching.Regex

def extractTwoDigitNumber(s: String): Option[String] =
    val regex: Regex = """\b\d{2}\b""".r
    regex.findFirstIn(s)

def extractFourDigitNumber(s: String): Option[String] =
    val regex: Regex = """(\d{4})""".r
    regex.findFirstIn(s)

def extractDate(s: String): Option[Instant] = {
    val pattern: Regex = """(\d{1,2})\s+([A-Za-z]+)\s+(\d{4})""".r
    pattern.findFirstMatchIn(s).map { m =>
        val day = m.group(1).toInt
        val month = m.group(2)
        val year = m.group(3).toInt
        val dateStr = s"$month $day $year 00:00:00"
        val formatter = DateTimeFormatter
            .ofPattern("[MMMM][MMM] d yyyy HH:mm:ss")
            .withZone(ZoneOffset.UTC)
        val t = formatter.parse(dateStr)
        ZonedDateTime.from(t).toInstant()
    }
}

def extractDashNumber(s: String): Option[String] = {
    extractDate(s) match {
        case Some(date) =>
            val f = DateTimeFormatter.ofPattern("yyyyMMdd")
            Some(f.format(date.atZone(ZoneOffset.UTC)))
        case None =>
            val regex1: Regex = """-(\d+)""".r
            regex1.findFirstMatchIn(s).map(_.group(1))
    }
}
                
def findStartTimestamp(videoArchive: VideoArchive): Option[Instant] =
    val videoFrames = Option(videoArchive.getVideoFrames)
        .map(_.asScala)
        .getOrElse(Nil)
    
    val earliestFrameTime = videoFrames
        .flatMap(f => Option(f.getRecordedDate))
        .map(_.toInstant)
        .minOption
    
    earliestFrameTime
        .orElse(extractDate(videoArchive.getName))
        .orElse(Some(Instant.EPOCH))

trait VideoArchiveTransform:

    val UriPrefix = "urn:imagecollection:org.mbari:"

    def canTransform(videoArchive: VideoArchive): Boolean
    def transform(videoArchive: VideoArchive): Option[Media]
    def cameraId: String

object BiauvTransform extends VideoArchiveTransform:

    private val pattern   = """.*-(\d{4})\.(\d{3})\.(\d{2})""".r
    private val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")

    override def cameraId: String = "Benthic Imaging AUV"

    override def canTransform(videoArchive: VideoArchive): Boolean =
        videoArchive.getName.toUpperCase().startsWith("BIAUV")

    override def transform(videoArchive: VideoArchive): Option[Media] =
        pattern
            .findFirstMatchIn(videoArchive.getName)
            .map { m =>
                val year              = m.group(1)
                val dayOfYear         = m.group(2)
                val sequence          = m.group(3)
                val timestamp         = Instants.asInstant(year.toInt, dayOfYear.toInt)
                val videoSequenceName = s"BIAUV ${formatter.format(timestamp.atZone(ZoneOffset.UTC))} $sequence"
                val uriString         = s"$UriPrefix${videoSequenceName.replace(" ", "_").replace("-", "_")}"
                val uri               = URI.create(uriString)
                val media             = Media()
                media.setVideoSequenceName(videoSequenceName)
                media.setVideoName(videoSequenceName)
                media.setCameraId(cameraId)
                media.setUri(uri)
                findStartTimestamp(videoArchive).foreach(media.setStartTimestamp)
                media.setVideoDescription(
                    s"BIAUV deployment ${videoArchive.getName} imported from VARS_Images."
                )
                media
            }

object SimpaTransform extends VideoArchiveTransform:

    override def cameraId: String = "SIMPA"

    override def canTransform(videoArchive: VideoArchive): Boolean =
        videoArchive.getName.toUpperCase().contains("SIMPA")

    override def transform(videoArchive: VideoArchive): Option[Media] =
        val deployment        = extractFourDigitNumber(videoArchive.getName).getOrElse("0000")
        val videoSequenceName = s"Ventana $deployment - SIMPA"
        val uriString         = s"$UriPrefix${videoArchive.getName.replace(" ", "_").replace("-", "_")}"
        val uri               = URI.create(uriString)
        val media             = Media()
        media.setVideoSequenceName(videoSequenceName)
        media.setVideoName(videoArchive.getName)
        media.setCameraId(cameraId)
        media.setUri(uri)
        findStartTimestamp(videoArchive).foreach(media.setStartTimestamp)
        media.setVideoDescription(
            s"SIMPA deployment ${videoArchive.getName} imported from VARS_Images."
        )
        Some(media)

trait StationMVideoArchiveTransform extends VideoArchiveTransform:

    def toVideoSequenceName(videoArchive: VideoArchive): Option[String] =
        val deployment = extractTwoDigitNumber(videoArchive.getName)
        extractDashNumber(videoArchive.getName) match
            case Some(value) =>
                deployment.map(d => s"Station M Pulse $d-$value - $cameraId")
            case None        =>
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

    override def cameraId: String = "Tripod Far Field M"

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
            val videoSequenceName = s"Station M Pulse $deployment - $cameraId"
            val videoArchiveName  = extractDashNumber(videoArchive.getName) match
                case Some(dashNumber) => s"Tripod Pulse $deployment-$dashNumber - $cameraId"
                case None             => s"Tripod Pulse $deployment - $cameraId"

            val uriString = s"$UriPrefix${videoSequenceName.replace(" ", "_").replace("-", "")}"
            val uri       = URI.create(uriString)
            val media     = Media()
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
        "CameraSled 803"              -> MediaBuilder()
            .withVideoSequenceName("Station M - Camera Sled 803")
            .withVideoName("Camera Sled 803")
            .withCameraId("Camera Sled")
            .withUri(URI.create("urn:imagecollection:org.mbari:Camera_Sled_803"))
            .withVideoDescription("CameraSled 803 imported from VARS_Images.")
            .build(),
        "MacroCam_MARS_Sept_2021"     -> MediaBuilder()
            .withVideoSequenceName("Macro Cam 202109")
            .withVideoName("Macro Cam 202109")
            .withCameraId("Macro Cam")
            .withUri(URI.create("urn:imagecollection:org.mbari:Macro_Cam_202109"))
            .withVideoDescription("MacroCam_MARS_Sept_2021 imported from VARS_Images.")
            .build(),
        // VideoArchive name -> CameraId
        "PAP-JC062-119"               -> MediaBuilder()
            .withVideoSequenceName("Porcupine Abyssal Plain - Time Lapse JC062-119")
            .withVideoName("PAP JC062-119 Time Lapse")
            .withCameraId("PAP Time Lapse Camera")
            .withUri(URI.create("urn:imagecollection:org.mbari:Porcupine_Abyssal_Plain__Time_Lapse_JC062_119"))
            .withVideoDescription("PAP-JC062-119 imported from VARS_Images.")
            .build(),
        "PAP-JC071-043"               -> MediaBuilder()
            .withVideoSequenceName("Porcupine Abyssal Plain - Time Lapse JC071-043")
            .withVideoName("PAP JC071-043 Time Lapse")
            .withCameraId("PAP Time Lapse Camera")
            .withUri(URI.create("urn:imagecollection:org.mbari:Porcupine_Abyssal_Plain__Time_Lapse_JC071_043"))
            .withVideoDescription("PAP-JC071-043 imported from VARS_Images.")
            .build(),
        "Rover Transit Pulse 61 good" -> MediaBuilder()
            .withVideoSequenceName("Station M Pulse 61 - Benthic Rover Transit Camera")
            .withVideoName("Station M Pulse 61 - Benthic Rover Transit Camera - good")
            .withCameraId("Benthic Rover Transit Camera")
            .withUri(URI.create("urn:imagecollection:org.mbari:Station_M_Pulse_61__Benthic_Rover_Transit_Camera__good"))
            .withVideoDescription("Rover Transit Pulse 61 good imported from VARS_Images.")
            .build(),
        "Rover_transit_Pulse_58_Ken"  -> MediaBuilder()
            .withVideoSequenceName("Station M Pulse 58 - Benthic Rover Transit Camera")
            .withVideoName("Station M Pulse 58 - Benthic Rover Transit Camera - Ken")
            .withCameraId("Benthic Rover Transit Camera")
            .withUri(URI.create("urn:imagecollection:org.mbari:Station_M_Pulse_58__Ken__Benthic_Rover_Transit_Camera"))
            .withVideoDescription("Rover_transit_Pulse_58_Ken imported from VARS_Images.")
            .build(),
        "Rover_transit_Pulse_59_Ken"  -> MediaBuilder()
            .withVideoSequenceName("Station M Pulse 59 - Benthic Rover Transit Camera")
            .withVideoName("Station M Pulse 59 - Benthic Rover Transit Camera - Ken")
            .withCameraId("Benthic Rover Transit Camera")
            .withUri(URI.create("urn:imagecollection:org.mbari:Station_M_Pulse_59__Ken__Benthic_Rover_Transit_Camera"))
            .withVideoDescription("Rover_transit_Pulse_59_Ken imported from VARS_Images.")
            .build(),
        "Rover_transit_Pulse_60_Ken"  -> MediaBuilder()
            .withVideoSequenceName("Station M Pulse 60 - Benthic Rover Transit Camera")
            .withVideoName("Station M Pulse 60 - Benthic Rover Transit Camera - Ken")
            .withCameraId("Benthic Rover Transit Camera")
            .withUri(URI.create("urn:imagecollection:org.mbari:Station_M_Pulse_60__Ken__Benthic_Rover_Transit_Camera"))
            .withVideoDescription("Rover_transit_Pulse_60_Ken imported from VARS_Images.")
            .build(),
        "Rover_transit_Pulse_71"      -> MediaBuilder()
            .withVideoSequenceName("Station M Pulse 71 - Benthic Rover Transit Camera")
            .withVideoName("Station M Pulse 71 - Benthic Rover Transit Camera - Ken")
            .withCameraId("Benthic Rover Transit Camera")
            .withUri(URI.create("urn:imagecollection:org.mbari:Station_M_Pulse_71__Ken__Benthic_Rover_Transit_Camera"))
            .withVideoDescription("Rover_transit_Pulse_71 imported from VARS_Images.")
            .build(),
        "Rover_transit_Pulse_72"      -> MediaBuilder()
            .withVideoSequenceName("Station M Pulse 72 - Benthic Rover Transit Camera")
            .withVideoName("Station M Pulse 72 - Benthic Rover Transit Camera")
            .withCameraId("Benthic Rover Transit Camera")
            .withUri(URI.create("urn:imagecollection:org.mbari:Station_M_Pulse_72__Benthic_Rover_Transit_Camera"))
            .withVideoDescription("Rover_transit_Pulse_72 imported from VARS_Images.")
            .build(),
        "S0055-01-tripod"             -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 55 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 55-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_55-S1__Tripod_Far_Field_M"))
            .withVideoDescription("S0055-01-tripod imported from VARS_Images.")
            .build(),
        "S6510-01-tripod"             -> MediaBuilder()
            .withCameraId("Tripod Far Field S")
            .withVideoSequenceName("Station M Pulse 65 - Tripod Far Field S")
            .withVideoName("Tripod Pulse 65-10-01 - Tripod Far Field S")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_65-S1__Tripod_Far_Field_S"))
            .withVideoDescription("S6510-01-tripod imported from VARS_Images.")
            .build(),
        "SES Pulse 70_Autolevels"     -> MediaBuilder()
            .withVideoSequenceName("Station M Pulse 70 - Sedimentation Event Sensor - Autolevels")
            .withVideoName("Station M Pulse 70 - Sedimentation Event Sensor - Autolevels")
            .withCameraId("Sedimentation Event Sensor")
            .withUri(
                URI.create("urn:imagecollection:org.mbari:Station_M_Pulse_70__Sedimentation_Event_Sensor__Autolevels")
            )
            .withVideoDescription("SES Pulse 70_Autolevels imported from VARS_Images.")
            .build(),
        "T0510-01-tripod"             -> MediaBuilder()
            .withCameraId("Tripod Far Field S")
            .withVideoSequenceName("Station M Pulse 51 - Tripod Far Field S")
            .withVideoName("Tripod Pulse 51-0510-01 - Tripod Far Field S")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_51_0510_01__Tripod_Far_Field_S"))
            .withVideoDescription("T0510-01-tripod imported from VARS_Images.")
            .build(),
        "T0715-01-tripod"             -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 71 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 71-0715-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_71_0715_01__Tripod_Far_Field_M"))
            .withVideoDescription("T0715-01-tripod imported from VARS_Images.")
            .build(),
        "T17021-01-tripod"            -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 17 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 17-17021-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_17_17021_01__Tripod_Far_Field_M"))
            .withVideoDescription("T17021-01-tripod imported from VARS_Images.")
            .build(),
        "T2231-01-tripod"             -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 22 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 22-2231-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_22_2231_01__Tripod_Far_Field_M"))
            .withVideoDescription("T2231-01-tripod imported from VARS_Images.")
            .build(),
        "T22311-01-tripod"            -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 22 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 22-22311-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_22_22311_01__Tripod_Far_Field_M"))
            .withVideoDescription("T22311-01-tripod imported from VARS_Images.")
            .build(),
        "T26121-01-tripod"            -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 26 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 26-26121-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_26_26121_01__Tripod_Far_Field_M"))
            .withVideoDescription("T26121-01-tripod imported from VARS_Images.")
            .build(),
        "T7021-01-tripod"             -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 70 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 70-7021-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_70_7021_01__Tripod_Far_Field_M"))
            .withVideoDescription("T7021-01-tripod imported from VARS_Images.")
            .build(),
        "T70211-01-tripod"            -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 70 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 70-70211-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_70_70211_01__Tripod_Far_Field_M"))
            .withVideoDescription("T70211-01-tripod imported from VARS_Images.")
            .build(),
        "T70212011-01-tripod"         -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 70 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 70-70212011-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_70_70212011_01__Tripod_Far_Field_M"))
            .withVideoDescription("T70212011-01-tripod imported from VARS_Images.")
            .build(),
        "T7022-01-tripod"             -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 70 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 70-7022-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_70_7022_01__Tripod_Far_Field_M"))
            .withVideoDescription("T7022-01-tripod imported from VARS_Images.")
            .build(),
        "T70221-01-tripod"            -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 70 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 70-70221-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_70_70221_01__Tripod_Far_Field_M"))
            .withVideoDescription("T70221-01-tripod imported from VARS_Images.")
            .build(),
        "T7023-01-tripod"             -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 70 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 70-7023-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_70_7023_01__Tripod_Far_Field_M"))
            .withVideoDescription("T7023-01-tripod imported from VARS_Images.")
            .build(),
        "T70231-01-tripod"            -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 70 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 70-70231-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_70_70231_01__Tripod_Far_Field_M"))
            .withVideoDescription("T70231-01-tripod imported from VARS_Images.")
            .build(),
        "T7024-01-tripod"             -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 70 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 70-7024-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_70_7024_01__Tripod_Far_Field_M"))
            .withVideoDescription("T7024-01-tripod imported from VARS_Images.")
            .build(),
        "T70241-01-tripod"            -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 70 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 70-7024-01 - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_70_7024_01__Tripod_Far_Field_M"))
            .withVideoDescription("T70241-01-tripod imported from VARS_Images.")
            .build(),
        "T7107-01-tripod"             -> MediaBuilder()
            .withCameraId("Tripod Far Field Solo")
            .withVideoSequenceName("Station M Pulse 71 - Tripod Far Field S")
            .withVideoName("Tripod Pulse 71-7107-01 - Tripod Far Field S")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_71_7107_01__Tripod_Far_Field_S"))
            .withVideoDescription("T7107-01-tripod imported from VARS_Images.")
            .build(),
        "Tripod Pulse 52-1-FILM"      -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 52 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 52-1-FILM - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_52_1_FILM__Tripod_Far_Field_M"))
            .withVideoDescription("Tripod Pulse 52-1-FILM imported from VARS_Images.")
            .build(),
        "Tripod Pulse 52-2-FILM"     -> MediaBuilder()
            .withCameraId("Tripod Far Field M")
            .withVideoSequenceName("Station M Pulse 52 - Tripod Far Field M")
            .withVideoName("Tripod Pulse 52-2-FILM - Tripod Far Field M")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_52_2_FILM__Tripod_Far_Field_M"))
            .withVideoDescription("Tripod Pulse 52-2-FILM imported from VARS_Images.")
            .build(),
        "Tripod Pulse 52-FILM-1"     -> MediaBuilder()
            .withCameraId("Tripod Near Field")
            .withVideoSequenceName("Station M Pulse 52 - Tripod Near Field")
            .withVideoName("Tripod Pulse 52-1A-FILM - Tripod Near Field")
            .withUri(URI.create("urn:imagecollection:org.mbari:Tripod_Pulse_52_1A_FILM__Tripod_Near_Field"))
            .withVideoDescription("Tripod Pulse 52-FILM-1 imported from VARS_Images.")
            .build()
    )

    override def cameraId: String = ???

    override def canTransform(videoArchive: VideoArchive): Boolean =
        specialCases.contains(videoArchive.getName)

    override def transform(videoArchive: VideoArchive): Option[Media] =
        specialCases.get(videoArchive.getName).map(m => {
            // Set start timestamp if available
            findStartTimestamp(videoArchive).foreach(m.setStartTimestamp)
            m
        })
