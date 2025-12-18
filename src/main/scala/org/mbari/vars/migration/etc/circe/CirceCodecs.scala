/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.etc.circe

import io.circe.*
import io.circe.generic.semiauto.*
import org.mbari.vars.migration.domain.*
import org.mbari.vars.migration.util.HexUtil

import java.net.{URI, URL}
import scala.util.Try

object CirceCodecs:

    given byteArrayEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]]:
        final def apply(xs: Array[Byte]): Json =
            Json.fromString(HexUtil.toHex(xs))
    given byteArrayDecoder: Decoder[Array[Byte]] = Decoder
        .decodeString
        .emapTry(str => Try(HexUtil.fromHex(str)))

    given urlDecoder: Decoder[URL] = Decoder
        .decodeString
        .emapTry(str => Try(URI.create(str).toURL))
    given urlEncoder: Encoder[URL] = Encoder
        .encodeString
        .contramap(_.toString)

    given authorizationSCDecoder: Decoder[org.mbari.vars.migration.domain.AuthorizationSC] = deriveDecoder
    given authorizationSCEncoder: Encoder[org.mbari.vars.migration.domain.AuthorizationSC] = deriveEncoder
