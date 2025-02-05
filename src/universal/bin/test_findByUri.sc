#!/usr/bin/env -S scala shebang

//> using dep "org.mbari.vars:vampire-squid-java-sdk:0.0.1"


import org.mbari.vars.vampiresquid.sdk.VampireSquidFactory
import org.mbari.vars.vampiresquid.sdk.r1.VampireSquidKiotaClient
import java.net.URI
import org.mbari.vars.vampiresquid.sdk.kiota.models.NotFound

val baseUrl = args(0)
val apiKey = args(1)
val url = args(2)


//val vampireSquid = VampireSquidFactory.create(baseUrl, apiKey)
val mediaService = VampireSquidKiotaClient(URI.create(baseUrl), apiKey)
try
    val uri = mediaService.findByUri(URI.create(url)).get()
    println(uri)
catch {
    case e: Exception =>
            e.getCause match
                case e: NotFound => println("Resource not found")
                case _ => println("Ouch! Non recoverable error")

}


