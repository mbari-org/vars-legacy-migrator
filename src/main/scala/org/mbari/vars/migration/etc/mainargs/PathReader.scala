/*
 * Copyright (c) Monterey Bay Aquarium Research Institute 2022
 *
 * vars-migration code is non-public software. Unauthorized copying of this file,
 * via any medium is strictly prohibited. Proprietary and confidential.
 */

package org.mbari.vars.migration.etc.mainargs

import mainargs.TokensReader

import java.nio.file.{Path, Paths}

/**
 * A simple [[mainargs.TokensReader]] for [[java.nio.file.Path]]s.
 */
object PathReader extends TokensReader.Simple[java.nio.file.Path]:

    def shortName: String                                      = "path"
    override def read(strs: Seq[String]): Either[String, Path] = Right(
        Paths
            .get(strs.head)
            .normalize()
            .toAbsolutePath()
    )
