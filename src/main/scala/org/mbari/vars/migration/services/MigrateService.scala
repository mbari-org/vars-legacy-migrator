package org.mbari.vars.migration.services

import vars.ToolBelt
import vars.annotation.VideoArchive

class MigrateService(toolBelt: ToolBelt):

    def migrate(videoArchive: VideoArchive, missionContact: String): Unit =
        println(s"Migrating ${videoArchive.getName} with missionContact $missionContact")
        // Do the migration here
        println("Migration complete")
        println("")
