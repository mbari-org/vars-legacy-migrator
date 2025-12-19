/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.subcommands

import org.mbari.vars.migration.services.raziel.Settings
import vars.{ToolBelt, ToolBeltFactory}

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.sql.DriverManager
import scala.util.{Failure, Success, Try, Using}
import org.mbari.vars.migration.etc.jdk.Loggers.given

object Configure:

    private val SETTINGS_FILE_NAME = "vars-legacy-jdbc-params.txt"

    private val log = System.getLogger(getClass.getName)

    def run(): Unit =
        println("Configure database connection to a legacy VARS database ...")
        val console  = System.console();
        val jdbcUrl  = console.readLine("JDBC URL: ");
        val username = console.readLine("Database Username: ");
        val pwd      = console.readPassword("Database Password: ");
        // Test connection
        println("Testing connection ...")

        val password = new String(pwd)
        if test(jdbcUrl, username, password) then
            println("Connection successful!")
            save(jdbcUrl, username, password)
            println("Saved connection parameters.")
        else println("Connection failed. Please check your JDBC URL and credentials.")

    def save(jdbcUrl: String, user: String, password: String): Unit =
        val settingsDirectory = Settings.getSettingsDirectory
        val aes               = Settings.getAes
        val file              = settingsDirectory.resolve(SETTINGS_FILE_NAME)
        val s                 = s"$jdbcUrl\n${aes.encrypt(user)}\n${aes.encrypt(password)}"
        Files.writeString(file, s, StandardCharsets.UTF_8)

    def test(jdbcUrl: String, username: String, password: String): Boolean =
        Using(DriverManager.getConnection(jdbcUrl, username, new String(password))) { conn =>
            val stmt  = conn.createStatement()
            stmt.execute("SELECT 1")
            val rs    = stmt.getResultSet
            rs.next()
            val count = rs.getInt(1)
            count == 1
        } match
            case Failure(e) =>
                log.atWarn.withCause(e).log("Failed to test connection to the legacy database")
                false
            case _          =>
                true

    def load(): Option[ToolBelt] =
        val settingsDirectory = Settings.getSettingsDirectory
        val aes               = Settings.getAes
        val file              = settingsDirectory.resolve(SETTINGS_FILE_NAME)
        if Files.exists(file) then
            val text    = Files.readString(file, StandardCharsets.UTF_8)
            val parts   = text.split("\n")
            val jdbcUrl = parts(0)
            val user    = aes.decrypt(parts(1))
            val pwd     = aes.decrypt(parts(2))
            Try {
                ToolBeltFactory.newToolBelt(jdbcUrl, user, pwd)
            } match
                case Failure(exception) =>
                    log.atWarn.withCause(exception).log("Failed to load jdbc params to the legacy database")
                    None
                case Success(value)     =>
                    if test(jdbcUrl, user, pwd) then
                        log.atDebug.log("Loaded jdbc params to the legacy database from $file")
                        Some(value)
                    else
                        log.atWarn.log(s"Failed to connect to database at $jdbcUrl using params loaded from $file")
                        None
        else None
