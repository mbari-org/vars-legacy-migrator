/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.etc.methanol

import com.github.mizosoft.methanol.Methanol
import io.circe.Decoder
import org.mbari.vars.migration.etc.circe.CirceBodyHandler

import java.net.URI
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest}
import java.nio.file.Path
import java.util.concurrent.Executors

/**
 * The Http object provides a simple interface for making HTTP GET requests using the JDK HttpClient.
 */
object Http:

    val Client: HttpClient = Methanol
        .newBuilder()
        .autoAcceptEncoding(true)
        .executor(Executors.newVirtualThreadPerTaskExecutor())
        .build()

    def getJson[T: Decoder](uri: String): Either[Throwable, T] =
        getJson(URI.create(uri))

    def getJson[T: Decoder](uri: URI): Either[Throwable, T] =
        val request = HttpRequest
            .newBuilder()
            .uri(uri)
            .header("Accept", "application/json")
            .GET()
            .build()

        val response = Client.send(request, CirceBodyHandler.of[T])

        if response.statusCode() == 200 then response.body()
        else
            Left(
                new Exception(
                    s"GET $uri failed. Response: ${response.body()}"
                )
            )

    def getAndSaveToFile(uri: URI, file: Path): Either[Throwable, Unit] =
        val request = HttpRequest
            .newBuilder()
            .uri(uri)
            .GET()
            .build()

        val response = Client.send(request, BodyHandlers.ofFile(file))

        if response.statusCode() == 200 then Right(())
        else
            Left(
                new Exception(
                    s"GET $uri failed. Response: ${response.body()}"
                )
            )
