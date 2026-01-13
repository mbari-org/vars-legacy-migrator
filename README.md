![MBARI logo](src/docs/_assets/images/logo-mbari-3b.png)

# vars-legacy-migrator

![Unit Tests](https://github.com/mbari-org/vars-legacy-migrator/actions/workflows/test.yml/badge.svg) ![Site Generator](https://github.com/mbari-org/vars-legacy-migrator/actions/workflows/docs.yml/badge.svg)

<https://mbari-org.github.io/vars-legacy-migrator/docs>

## Overview

vars-legacy-migrator is a Scala 3 CLI tool for migrating legacy VARS (Video Annotation and Reference System) annotation data from a legacy SQL Server database to modern microservices. This is proprietary internal software for MBARI (Monterey Bay Aquarium Research Institute).

### Data Flow

1. **Legacy Source**: Reads from SQL Server database (VARS legacy)
2. **Transformation**: Converts VideoArchives and Observations to modern Media and Annotations
3. **Target Services**: Writes to [VampireSquid](https://github.com/mbari-org/vampire-squid) (media metadata) and [Annosaurus](https://github.com/mbari-org/annosaurus) (annotations)

## Building

This project uses SBT (Scala Build Tool) with Scala 3.

```bash
# Compile the project
sbt compile

# Run tests
sbt test

# Package the application (creates executable)
sbt stage

# Build a distribution package
sbt universal:packageBin

# Run code formatter
sbt scalafmt

# Run linter
sbt scalafix

# Clean build artifacts
sbt clean
```

## Usage

After running `sbt stage`, the executable is available at `target/universal/stage/bin/vars-legacy-migrator`.

### Setup (Run First)

Before running any migration commands, you must configure the database connection and authenticate with the target services.

#### 1. Configure Legacy Database Connection

```bash
./vars-legacy-migrator configure
```

This command interactively prompts for:
- **JDBC URL**: The connection string for the legacy SQL Server database
- **Database Username**: Your database username
- **Database Password**: Your database password

The connection is tested before saving. Credentials are encrypted and stored locally.

#### 2. Login to Target Services

```bash
./vars-legacy-migrator login <razielServiceUrl>
```

This command authenticates with the Raziel service (MBARI's authentication gateway) to obtain credentials for accessing VampireSquid and Annosaurus. It prompts for:
- **Username**: Your MBARI username
- **Password**: Your MBARI password

Credentials are encrypted and stored locally for subsequent commands.

### Migration Commands

After setup is complete, you can run the following commands:

```bash
# Check health of target services
./vars-legacy-migrator service-health

# Migrate a single video archive
./vars-legacy-migrator migrate-one <videoArchiveName> <csvLookupPath>

# Migrate all video archives
./vars-legacy-migrator migrate-all <csvLookupPath>
```
