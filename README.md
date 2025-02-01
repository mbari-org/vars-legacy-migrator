![MBARI logo](src/docs/_assets/images/logo-mbari-3b.png)

# vars-legacy-migrator

![Unit Tests](https://github.com/mbari-org/vars-legacy-migrator/actions/workflows/test.yml/badge.svg) ![Site Generator](https://github.com/mbari-org/vars-legacy-migrator/actions/workflows/docs.yml/badge.svg)

Project for migrating first generation [VARS databases](https://github.com/hohonuuli/vars) to the [current infrastructure](https://github.com/mbari-org/m3-quickstart)

## Documentation

MBARI [sbt](https://www.scala-sbt.org) project compiled with [Scala 3](https://www.scala-lang.org)

## Usage

This is a normal sbt project. You can compile code with `sbt compile`, run it with `sbt run`, and `sbt console` will start a Scala 3 REPL.

## Useful Commands

1. `stage` - Build runnable project in `target/universal`
2. `universal:packageBin` - Build zip files of runnable project in `target/universal`
3. `scaladoc` - Build documentation, including API docs to `target/docs/site`
4. `compile` then `scalafmtAll` - Will convert all syntax to new-style, indent based Scala 3.


## Notes

Documentation can be added as markdown files in `docs` and will be included automatically when you run `scaladoc`.


