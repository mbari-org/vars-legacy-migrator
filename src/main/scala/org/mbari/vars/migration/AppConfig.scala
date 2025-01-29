/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration

import com.typesafe.config.ConfigFactory
import scala.util.Try

object AppConfig {

  val Config = ConfigFactory.load()

  val Name: String = "vars-migration"

  val Version: String = Try(getClass.getPackage.getImplementationVersion).getOrElse("0.0.0-SNAPSHOT")

}
