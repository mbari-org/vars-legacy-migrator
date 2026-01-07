/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.model

import org.mbari.vars.annosaurus.sdk.r1.models.{AncillaryData, Annotation, Association, ImageReference}
import org.mbari.vars.vampiresquid.sdk.r1.models.Media
import org.mbari.vcr4j.time.Timecode
import vars.annotation.VideoFrame
import vars.annotation.jpa.{CameraDataImpl, PhysicalDataImpl}

import java.net.URI
import scala.jdk.CollectionConverters.*
import vars.annotation.Observation

object AnnotationFactory:

    def toAnnotations(media: Media, videoFrame: VideoFrame, group: String): Seq[Annotation] =
        val observations = videoFrame.getObservations
        if observations == null then Nil
        else
            observations.asScala.toSeq.map { obs =>

                val cd                = Option(videoFrame.getCameraData).getOrElse(new CameraDataImpl())
                val pd                = Option(videoFrame.getPhysicalData).getOrElse(new PhysicalDataImpl())
                val recordedTimestamp = Option(videoFrame.getRecordedDate).map(_.toInstant).orNull
                val timecode          = Option(videoFrame.getTimecode).map(new Timecode(_)).orNull

                val pointOpt = remapPoint(obs)

                val associations = obs
                    .getAssociations
                    .asScala
                    .map(a => Association(a.getLinkName, a.getToConcept, a.getLinkValue))
                    .map(remapAssociation) // Remap old style localizations to new style
                    .toList

                val allAssociations = pointOpt match
                    case Some(point) => point :: associations
                    case None        => associations

                val imageReferences = Option(cd.getImageReference)
                    .map { ir =>
                        val i         = ImageReference()
                        i.setUrl(URI.create(ir).toURL)
                        val extension = ir.split('.').last.toLowerCase
                        val format    = extension match
                            case "jpg" | "jpeg" => "image/jpeg"
                            case "png"          => "image/png"
                            case "gif"          => "image/gif"
                            case "bmp"          => "image/bmp"
                            case "tiff" | "tif" => "image/tiff"
                            case _              => "image/unknown"
                        i.setFormat(format)
                        i
                    }
                    .toList
                    .asJava

                val ancillaryData = Option(pd).map { p =>
                    val ad = new AncillaryData()
                    Option(p.getDepth).foreach(d => ad.setDepthMeters(d.doubleValue()))
                    ad.setLatitude(p.getLatitude)
                    ad.setLongitude(p.getLongitude)
                    ad
                }.orNull

                val a = new Annotation()
                a.setVideoReferenceUuid(media.getVideoReferenceUuid)
                a.setActivity(cd.getDirection)
                a.setConcept(obs.getConceptName)
                a.setGroup(group)
                a.setRecordedTimestamp(recordedTimestamp)
                a.setTimecode(timecode)
                a.setObserver(obs.getObserver)
                a.setObservationTimestamp(obs.getObservationDate.toInstant)
                a.setAssociations(allAssociations.asJava)
                a.setImageReferences(imageReferences)
                a.setAncillaryData(ancillaryData)
                a
            }

    private def remapAssociation(association: Association): Association =
        val f = remapLine andThen remapPolygon
        f(association)


    private def remapPoint(observation: Observation): Option[Association] = 
        val targetLinkName = "localization-point" // linkValue is {x:[], y:[]}

        if observation.getX() != null && observation.getY() != null then
            val s = s"""{"x": [${math.round(observation.getX())}], "y":[${math.round(observation.getY())}]}"""
            Some(Association(targetLinkName, Association.VALUE_SELF, s, "application/json", null))
        else None



    private def remapLine(assocation: Association): Association =
        val sourceLinkName = "measurement in pixels [x0 y0 x1 y1 comment]"
        val targetLinkName = "localization-line" // linkValue is {x:[], y:[]}
        if assocation.getLinkName == sourceLinkName then
            val parts = assocation.getLinkValue.split(" ")
            val p0 = (math.round(parts(0).toDouble), math.round(parts(1).toDouble))
            val p1 = (math.round(parts(2).toDouble), math.round(parts(3).toDouble))
            val xsStr = s"[${p0._1}, ${p1._1}]"
            val ysStr = s"[${p0._2}, ${p1._2}]"
            val s = s"""{"x": $xsStr, "y": $ysStr}"""
            Association(targetLinkName, Association.VALUE_SELF, s, "application/json", null)
        else assocation

    private def remapPolygon(assocation: Association): Association = 
        val sourceLinkName = "area measurement coordinates [x0 y0 ... xn yn; comment]"
        val targetLinkName = "localization-polygon" // linkValue is {x:[], y:[]}
        if assocation.getLinkName == sourceLinkName then
            val s = assocation
                .getLinkValue
                .split(";")
                .head
                .trim
                .split(" ")
                .grouped(2)
                .map { case Array(x, y) => (math.round(x.toDouble), math.round(y.toDouble)) }
                .toSeq
                .unzip match
                case (xs, ys) =>
                    val xsStr = xs.mkString("[", ",", "]")
                    val ysStr = ys.mkString("[", ",", "]")
                    s"""{"x": $xsStr, "y": $ysStr}"""
            Association(targetLinkName, Association.VALUE_SELF, s, "application/json", null)
        else assocation


