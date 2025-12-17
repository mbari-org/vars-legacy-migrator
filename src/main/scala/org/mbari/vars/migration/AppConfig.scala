/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration

import com.typesafe.config.ConfigFactory
import org.mbari.vars.annosaurus.sdk.r1.{AnnosaurusHttpClient, AnnotationService}
import org.mbari.vars.vampiresquid.sdk.r1.{MediaService, VampireSquidKiotaClient}
import vars.ToolBelt

import java.net.URI
import java.time.Duration
import scala.util.Try

object AppConfig:

    val Config = ConfigFactory.load()

    val Name: String = "vars-legacy-migrator"

    val Version: String = Try(getClass.getPackage.getImplementationVersion).getOrElse("0.0.0-SNAPSHOT")

    object Annosaurus:
        val Url: String                       = Config.getString("annosaurus.url")
        val Secret: String                    = Config.getString("annosaurus.secret")
        def defaultService: AnnotationService = AnnosaurusHttpClient(Url, Duration.ofSeconds(20), Secret)

    object VampireSquid:
        val Url: String                  = Config.getString("vampiresquid.url")
        val Secret: String               = Config.getString("vampiresquid.secret")
        def defaultService: MediaService = VampireSquidKiotaClient(URI.create(Url), Secret)

    object VarsLegacy:
        def defaultToolBelt: ToolBelt = ToolBelt.defaultToolBelt()
