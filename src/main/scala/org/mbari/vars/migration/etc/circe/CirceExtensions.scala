/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.etc.circe

import io.circe.*

/**
 * This object contains extension methods for working with JSON objects via circe
 *
 * ```scala
 * import org.mbari.piscivore.etc.circe.CirceCodecs.given
 * import org.mbari.piscivore.etc.circe.CirceExtensions.*
 *
 * val json       = s"""{"count": 1, "video_reference_uuid": "123e4567-e89b-12d3-a456-426614174000"}"""
 * val count      = json.reify[CountSC] // From JSON
 * val jsonString = count.stringify     // To JSON
 * ```
 */
object CirceExtensions:

    private val printer = Printer.noSpaces.copy(dropNullValues = true)

    /**
     * Convert a circe Json object to a JSON string
     *
     * @param value
     *   Any value with an implicit circe coder in scope
     */
    extension (json: Json) def stringify: String = printer.print(json)

    /**
     * Convert an object to a JSON string
     *
     * @param value
     *   Any value with an implicit circe coder in scope
     */
    extension [T: Encoder](value: T)
        def stringify: String = Encoder[T]
            .apply(value)
            .deepDropNullValues
            .stringify

    extension [T: Decoder](jsonString: String) def toJson: Either[ParsingFailure, Json] = parser.parse(jsonString)

    extension (jsonString: String)
        def reify[T: Decoder]: Either[Error, T] =
            for
                json   <- jsonString.toJson
                result <- Decoder[T].apply(json.hcursor)
            yield result
