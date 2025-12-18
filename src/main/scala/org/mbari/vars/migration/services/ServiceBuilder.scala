/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services


import org.mbari.vars.annosaurus.sdk.r1.{AnnosaurusHttpClient, AnnotationService, VideoReferenceService}
import org.mbari.vars.migration.config.AppConfig
import org.mbari.vars.migration.etc.sdk.Futures.*
import org.mbari.vars.oni.sdk.OniFactory
import org.mbari.vars.oni.sdk.kiota.Oni
import org.mbari.vars.oni.sdk.r1.{ConceptService, OniKiotaClient, PreferencesService, UserService}
import org.mbari.vars.raziel.sdk.r1.RazielKiotaClient
import org.mbari.vars.raziel.sdk.r1.models.EndpointConfig
import org.mbari.vars.vampiresquid.sdk.VampireSquidFactory
import org.mbari.vars.vampiresquid.sdk.kiota.VampireSquid
import org.mbari.vars.vampiresquid.sdk.r1.{MediaService, VampireSquidKiotaClient}
import org.mbari.vars.migration.subcommands.Login

import java.net.URI
import java.time.Duration
import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.util.control.NonFatal
import java.util.concurrent.TimeUnit


/**
 * ServiceBuilder is a singleton object that provides access to various services such as AnnotationService,
 * MediaService, and OniService. It loads configurations from the Raziel service and initializes the services
 * accordingly.
 *
 * @param load
 *   A boolean flag indicating whether to load configurations from the Raziel service. Defaults to true. If set to
 *   false, the service configurations will be loaded from conf files and environment variables.
 */
class ServiceBuilder(load: Boolean = true):

    lazy val loadConfigurations: List[EndpointConfig] =
        given ExecutionContext = scala.concurrent.ExecutionContext.global
        if !load then List.empty[EndpointConfig]
        else
            try
                Login.load() match
                    case Some(params) =>
                        val urlString  = ServiceBuilder.adaptUrl(params.url().toString)
                        val uri        = URI.create(urlString)
                        val client     = new RazielKiotaClient(uri)
                        val bearerAuth = client.authenticate(params.username(), params.password()).join()
                        val services   = client.endpoints(bearerAuth.accessToken()).get(10, TimeUnit.SECONDS)
                        services.asScala.toList
                    case None         => Nil
            catch
                case NonFatal(e) =>
                    System.err.println(s"Error loading services: ${e.getMessage}")
                    e.printStackTrace()
                    List.empty[EndpointConfig]

    lazy val annotationServiceConfig: EndpointConfig =
        loadConfigurations.find(_.name() == "annosaurus") match
            case Some(endpoint) => endpoint
            case None           =>
                AppConfig.Annosaurus.toEndpointConfig("annosaurus")

    lazy val vampiresquidServiceConfig: EndpointConfig =
        loadConfigurations.find(_.name() == "vampire-squid") match
            case Some(endpoint) => endpoint
            case None           =>
                AppConfig.VampireSquid.toEndpointConfig("vampire-squid")

    lazy val oniServiceConfig: EndpointConfig =
        loadConfigurations.find(_.name() == "oni") match
            case Some(endpoint) => endpoint
            case None           =>
                AppConfig.Oni.toEndpointConfig("oni")

    lazy val annotationService: AnnotationService & VideoReferenceService =
        val config = annotationServiceConfig
        AnnosaurusHttpClient(
            config.url(),
            Duration.ofMillis(config.timeoutMillis()),
            config.secret()
        )

    lazy val vampireSquid: VampireSquid =
        val config = ServiceBuilder.adaptEndpointConfigForKiota(vampiresquidServiceConfig)
        VampireSquidFactory.create(config.url(), config.secret())

    lazy val mediaService: MediaService =
        val config = ServiceBuilder.adaptEndpointConfigForKiota(vampiresquidServiceConfig)
        new VampireSquidKiotaClient(vampireSquid)

    lazy val oni: Oni =
        val config = ServiceBuilder.adaptEndpointConfigForKiota(oniServiceConfig)
        OniFactory.create(config.url(), config.secret())

    lazy val oniService: ConceptService & UserService & PreferencesService =
        val config = ServiceBuilder.adaptEndpointConfigForKiota(oniServiceConfig)
        val uri    = URI.create(config.url())
        new OniKiotaClient(oni)

    /**
     * Use the save usernmae and password to authenticate against Oni and fetch the user details.
     */
    lazy val authenticatedUser: User =
        given ExecutionContext     = scala.concurrent.ExecutionContext.global
        val oni                    = OniService(oniServiceConfig)
        val razielConnectionParams = Login.load() match
            case Some(params) => params
            case None         =>
                throw new RuntimeException(
                    "No login information found. Please run the 'vars login' command to authenticate."
                )
        val authSc                 = oni.authorize(razielConnectionParams.username(), razielConnectionParams.password()).join()
        if authSc == null || authSc.access_token == null then
            throw new RuntimeException("Authorization failed. Please check your credentials.")
        oni.findAllUsers()
            .join()
            .find(_.username == razielConnectionParams.username())
            .getOrElse(
                throw new RuntimeException(s"User ${razielConnectionParams.username()} not found in Oni")
            )
        
            

object ServiceBuilder:

    /**
     * Kiota needs the URL to be without the trailing "/v1" or "/config" suffix. THis is a HACK to adapt the URL
     * accordingly.
     *
     * @param url
     * @return
     */
    def adaptUrl(url: String): String =
        if url.endsWith("/config") then url.substring(0, url.length - 7)
        else if url.endsWith("/v1") then url.substring(0, url.length - 3)
        else url

    /**
     * Adapts the EndpointConfig to ensure the URL is in the correct format for Kiota.
     *
     * @param endpointConfig
     * @return
     */
    def adaptEndpointConfigForKiota(endpointConfig: EndpointConfig): EndpointConfig =
        val fixedUrl = ServiceBuilder.adaptUrl(endpointConfig.url)
        EndpointConfig(endpointConfig.name(), fixedUrl, endpointConfig.timeoutMillis(), endpointConfig.secret())
