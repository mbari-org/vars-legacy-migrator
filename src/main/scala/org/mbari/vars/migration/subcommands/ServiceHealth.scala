/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import org.mbari.scommons.etc.jdk.Loggers.given
import org.mbari.vars.annosaurus.sdk.r1.AnnotationService
import org.mbari.vars.vampiresquid.sdk.r1.MediaService

import scala.util.control.NonFatal
import org.mbari.vars.migration.services.ServiceBuilder

object ServiceHealth:

    private val log = System.getLogger(getClass.getName)

    def run()(using annotationService: AnnotationService, mediaService: MediaService): Unit =

        println("1. Checking Raziel credentials (VARS API Gateway) ...")
        Login.loadRaw() match
            case Some(raw) => println(s"  Loading VARS configuration from ${raw.url()}")
            case None      => println("  No Raziel credentials found. Run `vars-legacy-migrator login` first.")

        val endpointConfigs = ServiceBuilder(true).loadConfigurations

        if (endpointConfigs.isEmpty) then
            println("  No service configurations found. Run `vars-legacy-migrator login` first or check that your login credentials are valid.")
        else
            endpointConfigs.foreach(cfg => println(s"  Loaded service configuration for: ${cfg.url}"))
        
        
        println("2. Checking service health ...")

        try
            annotationService.findActivities().join()
            println("  Connected to Annosaurus")
        catch
            case NonFatal(e) =>
                log.atError.withCause(e).log("Failed to connect to Annosaurus")
                println("  Failed to connect to Annosaurus")

        try
            mediaService.findAllCameraIds().join()
            println("  Connected to VampireSquid")
        catch
            case NonFatal(e) =>
                log.atError.withCause(e).log("Failed to connect to VampireSquid")
                println("  Failed to connect to VampireSquid")

        println("3. Checking legacy VARS database connection ...")
        Configure.loadRaw() match
            case Some((jdbcUrl, user, pwd)) => 
                println(s"  Connecting to a legacy VARS database at ${jdbcUrl}")
                if (Configure.test(jdbcUrl, user, pwd)) then
                    println("  Successfully connected to the legacy VARS database.")
                else
                    println("  Failed to connect to the legacy VARS database. Please run `vars-legacy-migrator configure` to set the connection parameters.")
            case None      => log.atWarn.log("No configuration found. Run `vars-legacy-migrator configure`` first.")



