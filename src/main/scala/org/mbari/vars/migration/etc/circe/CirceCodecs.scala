/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.etc.circe

import io.circe._
import io.circe.generic.semiauto._
import scala.util.Try
import java.net.URL
import org.mbari.vars.migration.util.HexUtil

object CirceCodecs:

  given byteArrayEncoder: Encoder[Array[Byte]] = new Encoder[Array[Byte]]:
    final def apply(xs: Array[Byte]): Json =
      Json.fromString(HexUtil.toHex(xs))
  given byteArrayDecoder: Decoder[Array[Byte]] = Decoder
    .decodeString
    .emapTry(str => Try(HexUtil.fromHex(str)))

  given urlDecoder: Decoder[URL] = Decoder
    .decodeString
    .emapTry(str => Try(new URL(str)))
  given urlEncoder: Encoder[URL] = Encoder
    .encodeString
    .contramap(_.toString)

  val CustomPrinter: Printer = Printer(
        dropNullValues = true,
        indent = ""
  )

  /** Convert a circe Json object to a JSON string
    *
    * @param value
    *   Any value with an implicit circe coder in scope
    */
  extension (json: Json) def stringify: String = CustomPrinter.print(json)

  /** Convert an object to a JSON string
    *
    * @param value
    *   Any value with an implicit circe coder in scope
    */
  extension [T: Encoder](value: T)
      def stringify: String = Encoder[T]
          .apply(value)
          .deepDropNullValues
          .stringify

  extension [T: Decoder](jsonString: String)
      def toJson: Either[ParsingFailure, Json] = parser.parse(jsonString);

  extension (jsonString: String)
      def reify[T: Decoder]: Either[Error, T] =
          for
              json   <- jsonString.toJson
              result <- Decoder[T].apply(json.hcursor)
          yield result

