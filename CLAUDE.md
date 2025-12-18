# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

vars-legacy-migrator is a Scala 3 CLI tool for migrating legacy VARS (Video Annotation and Reference System) annotation data from a legacy SQL Server database to modern microservices (Annosaurus and VampireSquid). This is proprietary internal software for MBARI (Monterey Bay Aquarium Research Institute).

## Build System

This project uses SBT (Scala Build Tool) with Scala 3.7.3.

### Common Commands

```bash
# Compile the project
sbt compile

# Run tests
sbt test

# Package the application
sbt stage

# Build a distribution package
sbt universal:packageBin

# Run scalafmt code formatter
sbt scalafmt

# Run scalafix linter
sbt scalafix

# Clean build artifacts
sbt clean
```

### Running the Application

After `sbt stage`, the executable is at `target/universal/stage/bin/vars-legacy-migrator`.

The application has three main subcommands:

```bash
# Check health of target services
./vars-legacy-migrator service-health

# Migrate a single video archive
./vars-legacy-migrator migrate-one <videoArchiveName> <csvLookupPath>

# Migrate all video archives
./vars-legacy-migrator migrate-all <csvLookupPath>
```

## Architecture

### Data Flow

1. **Legacy Source**: Reads from SQL Server database (VARS legacy) using `vars-legacy` SDK
2. **Transformation**: Converts VideoArchives and Observations to modern Media and Annotations
3. **Target Services**: Writes to VampireSquid (media metadata) and Annosaurus (annotations) via their SDKs

### Key Components

#### Services Layer (`org.mbari.vars.migration.services`)

- **VarsLegacyService**: Queries legacy VARS database via `ToolBelt` from vars-legacy SDK
  - Uses DAO pattern with JPA transactions
  - Eagerly loads VideoFrames, Observations, and Associations within transaction

- **VampireSquidService**: Manages Media records in VampireSquid
  - Creates new Media entries
  - Searches for existing Media by URI

- **MigrateService**: Core migration orchestration
  - Checks if migration is needed (no existing annotations)
  - Creates Media entries via VampireSquidService
  - Converts and batches Annotations (50 at a time)

- **VideoArchiveTransform**: Strategy pattern for different camera types
  - Each transform handles specific camera naming patterns (Station M, Tiburon, Rover, etc.)
  - Converts legacy VideoArchive names to normalized URIs and video sequence names
  - Implementations: GridPulseTransform, MacroCamTransform, RoverChamberPulseTransform, etc.

#### Model Layer (`org.mbari.vars.migration.model`)

- **MediaFactory**: Uses chain-of-responsibility pattern with VideoArchiveTransform implementations
  - Given a CSV lookup file for camera ID mappings (used by TripodPulseTransform)
  - Finds appropriate transform for each VideoArchive

- **AnnotationFactory**: Converts legacy VideoFrame → Observation to modern Annotation
  - Handles ancillary data (depth, lat/lon, camera direction)
  - Converts Associations (link triples)
  - Processes ImageReferences with proper MIME types

#### Subcommands (`org.mbari.vars.migration.subcommands`)

- **MigrateOne**: Migrates a single VideoArchiveSet by name
- **MigrateAll**: Iterates through all VideoArchives in legacy database
- **ServiceHealth**: Validates connectivity to target services

### Configuration

Configuration uses Typesafe Config (HOCON) loaded from environment variables:

**Legacy Database (SQL Server)**:
- `ANNOTATION_DATABASE_URL/USER/PASSWORD/DRIVER`
- `KNOWLEDGEBASE_DATABASE_URL/USER/PASSWORD/DRIVER`
- `MISC_DATABASE_URL/USER/PASSWORD/DRIVER`

**Target Services**:
- `ANNOSAURUS_URL` and `ANNOSAURUS_SECRET`
- `VAMPIRESQUID_URL` and `VAMPIRESQUID_SECRET`

Configuration files are in `src/universal/conf/` and get packaged with the app.

### Dependencies

Key external dependencies:
- **vars-legacy**: Legacy VARS JPA models and DAOs (proprietary)
- **annosaurus-java-sdk**: HTTP client for Annosaurus annotation service
- **vampire-squid-java-sdk**: Kiota-based client for VampireSquid media service
- **mainargs**: CLI argument parsing
- **circe**: JSON encoding/decoding
- **methanol**: HTTP client library
- **scommons**: MBARI common utilities

## Development Notes

### Context Injection

The codebase uses Scala 3's `using` (contextual parameters) for dependency injection:
- Services are provided as `given` instances in Main
- They propagate through the call chain automatically
- Important: MediaFactory requires a CSV lookup path provided per-command

### Transaction Management

VarsLegacyService uses explicit transaction boundaries:
- Call `startTransaction()` before queries
- Eagerly load all associations within the transaction
- Call `endTransaction()` and `close()` when done

### URI Format

Media URIs follow the pattern: `urn:imagecollection:org.mbari:<normalized_name>`
- Spaces become underscores
- Dashes are removed
- Example: "Station M Pulse 58 - Tripod Dual Far Field" → `urn:imagecollection:org.mbari:Station_M_Pulse_58_TripodDualFarField`

### Transform Strategy

When adding new camera types:
1. Create a new VideoArchiveTransform implementation
2. Implement `canTransform()` to match VideoArchive names
3. Implement `transform()` to produce Media with correct metadata
4. Add to MediaFactory's transforms sequence
