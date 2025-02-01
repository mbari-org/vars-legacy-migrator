/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.services

import vars.ToolBelt
import vars.annotation.VideoArchive

class MigrateService(toolBelt: ToolBelt):

    def migrate(videoArchive: VideoArchive, missionContact: String): Unit =
        println(s"Migrating ${videoArchive.getName} with missionContact $missionContact")
        // Do the migration here
        println("Migration complete")
        println("")
