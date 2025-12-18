/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.etc.circe

import io.circe.Decoder
import io.circe.parser.decode

import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * A CirceBodyHandler is a HttpResponse.BodyHandler that uses Circe to decode the response body to an Either where the
 * left side is a Throwable and the right side is the desired type T.
 * @tparam T
 *   The type to decode the response body to.
 */
class CirceBodyHandler[T: Decoder] extends HttpResponse.BodyHandler[Either[Throwable, T]]:

    override def apply(responseInfo: HttpResponse.ResponseInfo): HttpResponse.BodySubscriber[Either[Throwable, T]] =
        HttpResponse
            .BodySubscribers
            .mapping(HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8), s => decode[T](s))

object CirceBodyHandler:

    /**
     * Create a CirceBodyHandler for the desired type T. This follows the other BodyHandler conventions in the JDK.
     * Usage is:
     *
     * ```scala
     * given Decoder[MyType] = ...
     * val bodyHandler = CirceBodyHandler.of[MyType]
     * ```
     *
     * @tparam T
     *   The type to decode the response body to.
     * @return
     *   A CirceBodyHandler for the desired type T.
     */
    def of[T: Decoder]: CirceBodyHandler[T] = new CirceBodyHandler[T]()
