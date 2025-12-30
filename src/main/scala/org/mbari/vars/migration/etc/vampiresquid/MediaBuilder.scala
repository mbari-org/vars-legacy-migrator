package org.mbari.vars.migration.etc.vampiresquid


package org.mbari.vars.vampiresquid.sdk.r1.models

import java.net.URI
import java.time.{Duration, Instant}
import java.util.UUID
import _root_.org.mbari.vars.vampiresquid.sdk.r1.models.Media

case class MediaBuilder(
    videoSequenceUuid: Option[UUID] = None,
    videoReferenceUuid: Option[UUID] = None,
    videoUuid: Option[UUID] = None,
    videoSequenceName: Option[String] = None,
    cameraId: Option[String] = None,
    uri: Option[URI] = None,
    startTimestamp: Option[Instant] = None,
    durationMillis: Option[Duration] = None,
    container: Option[String] = None,
    width: Option[Int] = None,
    height: Option[Int] = None,
    frameRate: Option[Double] = None,
    sizeBytes: Option[Long] = None,
    sha512: Option[Array[Byte]] = None,
    videoCodec: Option[String] = None,
    audioCodec: Option[String] = None,
    description: Option[String] = None,
    videoName: Option[String] = None,
    videoDescription: Option[String] = None,
    videoSequenceDescription: Option[String] = None
) {
  
  def withVideoSequenceUuid(uuid: UUID): MediaBuilder = copy(videoSequenceUuid = Some(uuid))
  def withVideoReferenceUuid(uuid: UUID): MediaBuilder = copy(videoReferenceUuid = Some(uuid))
  def withVideoUuid(uuid: UUID): MediaBuilder = copy(videoUuid = Some(uuid))
  def withVideoSequenceName(name: String): MediaBuilder = copy(videoSequenceName = Some(name))
  def withCameraId(id: String): MediaBuilder = copy(cameraId = Some(id))
  def withUri(u: URI): MediaBuilder = copy(uri = Some(u))
  def withStartTimestamp(ts: Instant): MediaBuilder = copy(startTimestamp = Some(ts))
  def withDuration(d: Duration): MediaBuilder = copy(durationMillis = Some(d))
  def withContainer(c: String): MediaBuilder = copy(container = Some(c))
  def withWidth(w: Int): MediaBuilder = copy(width = Some(w))
  def withHeight(h: Int): MediaBuilder = copy(height = Some(h))
  def withFrameRate(fr: Double): MediaBuilder = copy(frameRate = Some(fr))
  def withSizeBytes(size: Long): MediaBuilder = copy(sizeBytes = Some(size))
  def withSha512(hash: Array[Byte]): MediaBuilder = copy(sha512 = Some(hash))
  def withVideoCodec(codec: String): MediaBuilder = copy(videoCodec = Some(codec))
  def withAudioCodec(codec: String): MediaBuilder = copy(audioCodec = Some(codec))
  def withDescription(desc: String): MediaBuilder = copy(description = Some(desc))
  def withVideoName(name: String): MediaBuilder = copy(videoName = Some(name))
  def withVideoDescription(desc: String): MediaBuilder = copy(videoDescription = Some(desc))
  def withVideoSequenceDescription(desc: String): MediaBuilder = copy(videoSequenceDescription = Some(desc))
  
  def build(): Media = {
    val media = new Media()
    videoSequenceUuid.foreach(media.setVideoSequenceUuid)
    videoReferenceUuid.foreach(media.setVideoReferenceUuid)
    videoUuid.foreach(media.setVideoUuid)
    videoSequenceName.foreach(media.setVideoSequenceName)
    cameraId.foreach(media.setCameraId)
    uri.foreach(media.setUri)
    startTimestamp.foreach(media.setStartTimestamp)
    durationMillis.foreach(media.setDuration)
    container.foreach(media.setContainer)
    width.foreach(w => media.setWidth(w))
    height.foreach(h => media.setHeight(h))
    frameRate.foreach(fr => media.setFrameRate(fr))
    sizeBytes.foreach(s => media.setSizeBytes(s))
    sha512.foreach(media.setSha512)
    videoCodec.foreach(media.setVideoCodec)
    audioCodec.foreach(media.setAudioCodec)
    description.foreach(media.setDescription)
    videoName.foreach(media.setVideoName)
    videoDescription.foreach(media.setVideoDescription)
    videoSequenceDescription.foreach(media.setVideoSequenceDescription)
    media
  }
}

object MediaBuilder {
  def apply(): MediaBuilder = new MediaBuilder()
}