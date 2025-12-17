/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services

import org.mbari.scommons.etc.jdk.Loggers.given
import org.mbari.vars.vampiresquid.sdk.kiota.models.NotFound
import org.mbari.vars.vampiresquid.sdk.r1.MediaService
import org.mbari.vars.vampiresquid.sdk.r1.models.Media

import java.net.URI

class VampireSquidService(using mediaService: MediaService):

    private val log = System.getLogger(getClass.getName)

    def findByUri(uri: URI): Option[Media] =
        try Option(mediaService.findByUri(uri).join())
        catch
            case e: Exception =>
                e.getCause match
                    case e: NotFound  =>
                        log.atInfo.log(s"Media not found for $uri")
                    case e: Exception =>
                        log.atError.withCause(e).log(s"An error occurred when attempting to find media for $uri")
                None

    def create(media: Media): Option[Media] =
        try Option(mediaService.create(media).join())
        catch
            case e: Exception =>
                log.atError.withCause(e).log(s"Failed to create media for ${media.getUri}")
                None
