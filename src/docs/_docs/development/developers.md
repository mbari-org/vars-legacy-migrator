# For Developers

## Build System

This is an MBARI [sbt](https://www.scala-sbt.org) project compiled with [Scala 3](https://www.scala-lang.org).

### Common Commands

| Command | Description |
|---------|-------------|
| `sbt compile` | Compile the project |
| `sbt test` | Run tests |
| `sbt stage` | Build runnable project in `target/universal/stage` |
| `sbt universal:packageBin` | Build zip distribution in `target/universal` |
| `sbt scaladoc` | Build documentation to `target/docs/site` |
| `sbt scalafmt` | Format code |
| `sbt scalafix` | Run linter |
| `sbt clean` | Clean build artifacts |

## Architecture

### Key Components

#### Services Layer (`org.mbari.vars.migration.services`)

- **VarsLegacyService**: Queries legacy VARS database via `ToolBelt` from vars-legacy SDK. Uses DAO pattern with JPA transactions.
- **VampireSquidService**: Manages Media records in VampireSquid. Creates new Media entries and searches for existing Media by URI.
- **MigrateService**: Core migration orchestration. Checks if migration is needed, creates Media entries, converts and batches Annotations.

#### Model Layer (`org.mbari.vars.migration.model`)

- **MediaFactory**: Uses chain-of-responsibility pattern with VideoArchiveTransform implementations to convert legacy VideoArchive names to normalized URIs.
- **AnnotationFactory**: Converts legacy VideoFrame/Observation to modern Annotation format, including ancillary data and associations.

#### Subcommands (`org.mbari.vars.migration.subcommands`)

- **Configure**: Sets up legacy database connection
- **Login**: Authenticates with Raziel for target service access
- **MigrateOne**: Migrates a single VideoArchiveSet
- **MigrateAll**: Iterates through all VideoArchives
- **ServiceHealth**: Validates connectivity to target services

### VideoArchive Transforms

The `VideoArchiveTransform` trait implements a strategy pattern for handling different camera types. Each implementation handles specific camera naming patterns and converts legacy names to normalized URIs.

When adding new camera types:

1. Create a new `VideoArchiveTransform` implementation
2. Implement `canTransform()` to match VideoArchive names
3. Implement `transform()` to produce Media with correct metadata
4. Add to `MediaFactory`'s transforms sequence

### URI Format

Media URIs follow the pattern: `urn:imagecollection:org.mbari:<normalized_name>`

- Spaces become underscores
- Dashes are removed

Example: `"Station M Pulse 58 - Tripod Dual Far Field"` becomes `urn:imagecollection:org.mbari:Station_M_Pulse_58_TripodDualFarField`

## Development Notes

### Context Injection

The codebase uses Scala 3's `using` (contextual parameters) for dependency injection. Services are provided as `given` instances in Main and propagate through the call chain automatically.

### Transaction Management

VarsLegacyService uses explicit transaction boundaries:

1. Call `startTransaction()` before queries
2. Eagerly load all associations within the transaction
3. Call `endTransaction()` and `close()` when done

## Documentation

Documentation can be added as markdown files in `src/docs` and will be included automatically when you run `sbt scaladoc`.
