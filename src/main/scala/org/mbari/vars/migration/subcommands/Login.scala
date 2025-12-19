/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import java.net.URI

import org.mbari.vars.migration.config.AppConfig
import org.mbari.vars.migration.services.ServiceBuilder
import org.mbari.vars.migration.services.raziel.{RazielConnectionParams, Settings}
import org.mbari.vars.raziel.sdk.r1.RazielKiotaClient

import scala.jdk.OptionConverters.*

object Login:

    private val SETTINGS_FILE_NAME: String = "raziel.txt"

    def run(razielServiceUrl: String): Int =
        println(s"Logging in to Raziel service at $razielServiceUrl")

        // HACK: Users will provide a URI that ends with config
        //       but the Kiota client needs the base URL
        val provideUri = URI.create(razielServiceUrl);
        val adaptedUri = URI.create(ServiceBuilder.adaptUrl(provideUri.toString))

        val client     = new RazielKiotaClient(adaptedUri)
        val console    = System.console();
        val username   = console.readLine("Username: ")
        val password   = console.readPassword("Password: ")
        val pwd        = new String(password)
        val bearerAuth = client.authenticate(username, pwd).join()
        if bearerAuth != null && bearerAuth.accessToken() != null then
            println(s"Login successful for user: $username")
            // Store the bearer token or use it for further requests
            // For example, you might want to set it in a config or pass it to other services
            // client.setBearerToken(bearerAuth.getAccessToken)
            Login.save(provideUri, username, pwd)
        else
            println("Login failed. Please check your credentials.")
            return 1 // Return an error code

        0

    def save(uri: URI, username: String, password: String): Unit =
        val settingsDirectory = Settings.getSettingsDirectory
        val aes               = Settings.getAes
        val file              = settingsDirectory.resolve(SETTINGS_FILE_NAME)
        val connectionParams  = new RazielConnectionParams(uri.toURL, username, new String(password))
        connectionParams.write(file, aes)
        println(s"Saved connection parameters to $file")

    def load(): Option[RazielConnectionParams] =
        loadRaw()
            .map(p =>
                // HACK: Users will provide a URI that ends with config
                //       but the Kiota client needs the base URL
                val adaptedUrl = ServiceBuilder.adaptUrl(p.url().toString)
                new RazielConnectionParams(URI.create(adaptedUrl).toURL, p.username, p.password())
            )

    def loadRaw(): Option[RazielConnectionParams] =
        val settingsDirectory = Settings.getSettingsDirectory
        val aes               = Settings.getAes
        val file              = settingsDirectory.resolve(SETTINGS_FILE_NAME)
        if file.toFile.exists() then
            RazielConnectionParams
                .read(file, aes)
                .toScala
        else None
