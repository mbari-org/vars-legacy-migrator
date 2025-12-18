/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.config

import org.mbari.vars.raziel.sdk.r1.models.EndpointConfig

import java.time.Duration

case class ServerConfig(
    endpoint: String,
    secret: Option[String] = None,
    timeout: Duration = Duration.ofSeconds(10)
):

    def toEndpointConfig(name: String): EndpointConfig =
        EndpointConfig(name, endpoint, timeout.toMillis, secret.getOrElse(""))
