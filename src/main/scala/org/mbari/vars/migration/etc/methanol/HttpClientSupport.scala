/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.etc.methanol

import com.github.mizosoft.methanol.Methanol
import io.circe.Decoder
import io.circe.parser.decode
import org.mbari.vars.migration.config.AppConfig

import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.Executor
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

/**
 * Helper for using javas' HttpClient.
 * @author
 *   Brian Schlining
 */
class HttpClientSupport(
    timeout: Duration = Duration.ofSeconds(10),
    executor: Executor = ExecutionContext.global
) extends AutoCloseable:

    val client = Methanol
        .newBuilder()
        .autoAcceptEncoding(true)
        .connectTimeout(timeout)
        .executor(executor)
        .interceptor(LoggingInterceptor)
        .readTimeout(timeout)
        .requestTimeout(timeout)
        .userAgent(AppConfig.Name)
        .build()

    def requestUnit(request: HttpRequest): Either[Throwable, Unit] = {
        //format: off
        for
            response <- Try(client.send(request, BodyHandlers.discarding())).toEither
            _        <- if response.statusCode() == 200 || response.statusCode() == 202 || response.statusCode() == 204 then
                            Right(())
            else Left(new RuntimeException(s"Unexpected response from ${request.uri}: ${response.body}"))
        yield ()
        //format: on
    }

    def requestObjects[T: Decoder](request: HttpRequest): Either[Throwable, T] =
        for
            body <- requestString(request)
            obj  <- decode[T](body)
        yield obj

    def requestString(request: HttpRequest): Either[Throwable, String] = {
        //format: off
        for
            response <- Try(client.send(request, BodyHandlers.ofString(StandardCharsets.UTF_8))).toEither
            body     <- if response.statusCode() == 200 || response.statusCode() == 202 || response.statusCode() == 204 then
                            Right(response.body)
            else Left(new RuntimeException(s"Unexpected response from ${request.uri}: ${response.body}"))
        yield body
        //format: on
    }

    def requestOption[T: Decoder](request: HttpRequest): Either[Throwable, Option[T]] =
        Try(client.send(request, BodyHandlers.ofString())) match
            case Failure(exception) => Left(exception)
            case Success(response)  =>
                if response.statusCode() == 200 || response.statusCode() == 202 || response.statusCode() == 204 then
                    decode[T](response.body) match
                        case Left(e)      =>
                            Left(
                                new RuntimeException(s"Failed to decode response from ${request.uri}: ${response.body}")
                            )
                        case Right(value) => Right(Some(value))
                else if response.statusCode() == 404 then Right(None)
                else
                    Left(
                        new RuntimeException(s"Unexpected response from ${request.uri}: ${response.body}")
                    )

    def close(): Unit = client.close()
