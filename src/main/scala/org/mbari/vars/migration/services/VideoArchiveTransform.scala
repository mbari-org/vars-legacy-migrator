/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services

import org.mbari.vars.vampiresquid.sdk.r1.models.Media
import vars.annotation.VideoArchive

import java.net.URI
import java.nio.file.Path
import java.time.Instant
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.matching.Regex

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

class TripodPulseTransform(csvLookup: Path) extends VideoArchiveTransform:

    // Read CSV file , first column is videoArhive name, second column is cameraId
    val cameraIdMap: Map[String, String] =
        Using(Source.fromFile(csvLookup.toFile)) { source =>
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
        val deployment = extractTwoDigitNumber(videoArchive.getName)
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
            media.setVideoName(videoSequenceName)
            media.setCameraId(cameraId)
            media.setUri(uri)
            findStartTimestamp(videoArchive).foreach(media.setStartTimestamp)
            media.setVideoDescription(
                s"Station M $cameraId deployment $deployment imported from VARS_Images.  Original VideoArchive name: ${videoArchive.getName}"
            )
            media

    def extractCameraId(videoArchiveName: String): String =
        if videoArchiveName.startsWith("TRIPOD PULSE") then
            if videoArchiveName.contains("DUAL") then "Tripod Dual Far Field"
            else if videoArchiveName.contains("NEA") then "Tripod Dual Near Field"
            else if videoArchiveName.contains("SOLO") then "Tripod Solo Far Field"
            else "Tripod Mixed Far Field"
        else "Unknown"
