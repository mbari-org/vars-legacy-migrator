package org.mbari.vars.migration.model

import org.mbari.vars.annosaurus.sdk.r1.models.{AncillaryData, Annotation, Association, ImageReference}
import org.mbari.vars.vampiresquid.sdk.r1.models.Media
import org.mbari.vcr4j.time.Timecode
import vars.annotation.VideoFrame
import vars.annotation.jpa.{CameraDataImpl, PhysicalDataImpl}

import java.net.URI
import scala.jdk.CollectionConverters.*

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

                val associations = obs
                    .getAssociations
                    .asScala
                    .map(a => Association(a.getLinkName, a.getToConcept, a.getLinkValue))
                    .toList

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
                a.setAssociations(associations.asJava)
                a.setImageReferences(imageReferences)
                a.setAncillaryData(ancillaryData)
                a
            }
