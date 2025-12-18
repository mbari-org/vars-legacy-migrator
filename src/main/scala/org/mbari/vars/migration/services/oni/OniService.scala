/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services

import io.circe.*
import io.circe.generic.semiauto.*
import org.mbari.vars.migration.domain.AuthorizationSC
import org.mbari.vars.migration.etc.circe.CirceCodecs.authorizationSCDecoder
import org.mbari.vars.migration.etc.circe.CirceExtensions.stringify
import org.mbari.vars.migration.etc.jdk.Loggers.given
import org.mbari.vars.migration.etc.methanol.HttpClientSupport
import org.mbari.vars.raziel.sdk.r1.models.EndpointConfig

import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.text.SimpleDateFormat
import java.time.{Duration, Instant}
import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

object HistoryConstants:
    val ActionAdd: String     = "ADD"
    val ActionDelete: String  = "DELETE"
    val ActionReplace: String = "REPLACE"

case class History(
    concept: String,
    creationTimestamp: Instant,
    creatorName: String,
    action: Option[String] = None,
    field: Option[String] = None,
    oldValue: Option[String] = None,
    newValue: Option[String] = None,
    approved: Boolean = false,
    processedTimestamp: Option[Instant] = None,
    processorName: Option[String] = None
):

    lazy val stringValue: String =
        val s0   = s" [$creationTimestamp by $creatorName] ${action.getOrElse("")} ${field.getOrElse("")}"
        val oldV = oldValue.map(v => s" '$v'").getOrElse("")
        val newV = newValue.map(v => s" '$v'").getOrElse("")
        val s1   = action.map:
            case HistoryConstants.ActionAdd     => newV
            case HistoryConstants.ActionDelete  => oldV
            case HistoryConstants.ActionReplace =>
                s"$oldV with $newV"
            case _                              => ""
        s0 + s1.getOrElse("")

case class User(
    username: String,
    password: String,
    role: String,
    id: Long,
    isEncrypted: Boolean,
    affiliation: Option[String] = None,
    firstName: Option[String] = None,
    lastName: Option[String] = None,
    email: Option[String] = None
):
    val isAdmin: Boolean      = role.toLowerCase == "admin"
    val isMaintainer: Boolean = role.toLowerCase == "maint"
    val isPowerUser: Boolean  = isAdmin || isMaintainer

/**
 * @author
 *   Brian Schlining
 * @since 2019-11-19T15:49:00
 */
class OniService(endpointConfig: EndpointConfig):

    private val log   = System.getLogger(getClass.getName)
    val timeout       = Duration.ofMillis(endpointConfig.timeoutMillis)
    val clientSupport = new HttpClientSupport(timeout)

    import OniService.given

    val DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

    def findAllUsers(): Future[Seq[User]] = get[Seq[User]](endpointConfig.url() + "/users")

    def findAdmins(): Future[Seq[User]] = findAllUsers().map(xs => xs.filter(_.role == "Admin"))

    def findPowerUsers(): Future[Seq[User]] =
        findAllUsers().map(xs => xs.filter(u => u.role == "Maint" || u.role == "Admin"))

    def findPendingHistories()(using ec: ExecutionContext): Future[Seq[History]] =
        val url = endpointConfig.url() + "/history/pending"
        get[Seq[History]](url)

    def findApprovedHistories()(using ec: ExecutionContext): Future[Seq[History]] =
        val url = endpointConfig.url() + "/history/approved"
        get[Seq[History]](url)

    def get[T](url: String)(using ec: ExecutionContext, decoder: Decoder[T]): Future[T] = Future {

        val request = HttpRequest
            .newBuilder(URI.create(url))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .build()
        clientSupport.requestObjects[T](request) match
            case Left(e)      =>
                log.atError.withCause(e).log(s"Failed to fetch histories from $url")
                throw e
            case Right(value) => value
    }

    def post[T, B](url: String, body: T)(using
        ec: ExecutionContext,
        encoder: Encoder[T],
        decoder: Decoder[B]
    ): Future[B] = Future {

        val request = HttpRequest
            .newBuilder(URI.create(url))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body.stringify))
            .build()
        clientSupport.requestObjects[B](request) match
            case Left(e)      =>
                log.atError.withCause(e).log(s"Failed to post to $url")
                throw e
            case Right(value) => value
    }

    def authorize(username: String, password: String)(using ec: ExecutionContext): Future[AuthorizationSC] =
        val url         = endpointConfig.url() + s"/auth/login"
        val auth        = username + ":" + password;
        val encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        val request = HttpRequest
            .newBuilder(URI.create(url))
            .timeout(timeout)
            .header("Authorization", "Basic " + encodedAuth)
            .POST(BodyPublishers.noBody())
            .build()

        clientSupport.requestObjects[AuthorizationSC](request) match
            case Left(e)      =>
                log.atError.withCause(e).log(s"Failed to authorize user $username")
                throw e
            case Right(value) => Future.successful(value)

object OniService:

    given HistoryDecoder: Decoder[History] = deriveDecoder[History]
    given HistoryEncoder: Encoder[History] = deriveEncoder[History]
    given UserDecoder: Decoder[User]       = deriveDecoder[User]
    given UserEncoder: Encoder[User]       = deriveEncoder[User]
