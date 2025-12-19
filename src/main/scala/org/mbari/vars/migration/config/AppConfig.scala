/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.config

import org.mbari.vars.migration.config.ServerConfig

import java.time.Duration
import scala.util.Try

/**
 * Reads configuration from the `application.conf`, `reference.conf`, or environment variables.
 */
object AppConfig:

    private val config = com.typesafe.config.ConfigFactory.load()

    val Name: String = "vars-cli"

    val Version: String =
        Try(getClass.getPackage.getImplementationVersion).getOrElse("0.0.0-SNAPSHOT")

    lazy val Annosaurus: ServerConfig =
        val endpoint =
            val ep = config.getString("annosaurus.endpoint")
            if ep.endsWith("/") then ep else s"$ep/"
        ServerConfig(
            endpoint,
            Option(config.getString("annosaurus.secret")),
            config.getDuration("annosaurus.timeout")
        )

    lazy val Oni: ServerConfig =
        val endpoint =
            val ep = config.getString("oni.endpoint")
            if ep.endsWith("/") then ep else s"$ep/"
        ServerConfig(
            endpoint,
            timeout = config.getDuration("oni.timeout")
        )

    lazy val Raziel: ServerConfig =
        val endpoint =
            val ep = config.getString("raziel.endpoint")
            if ep.endsWith("/") then ep else s"$ep/"
        ServerConfig(
            endpoint,
            Option(config.getString("raziel.secret")),
            config.getDuration("raziel.timeout")
        )

    lazy val VampireSquid: ServerConfig =
        val endpoint =
            val ep = config.getString("vampire.squid.endpoint")
            if ep.endsWith("/") then ep else s"$ep/"
        ServerConfig(
            endpoint,
            Option(config.getString("vampire.squid.secret")),
            config.getDuration("vampire.squid.timeout")
        )
