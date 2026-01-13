# User Guide

This guide covers installation, setup, and usage of the vars-legacy-migrator tool.

## Installation

### Prerequisites

- Java 21 or later
- Access to the legacy VARS SQL Server database
- Access to MBARI's Raziel authentication service
- Network access to VampireSquid and Annosaurus services

### Building from Source

```bash
# Clone the repository
git clone https://github.com/mbari-org/vars-legacy-migrator.git
cd vars-legacy-migrator

# Build the executable
sbt stage
```

The executable will be available at `target/universal/stage/bin/vars-legacy-migrator`.

### Distribution Package

Alternatively, build a distribution zip:

```bash
sbt universal:packageBin
```

The zip file will be in `target/universal/`.

## Setup

Before running any migration commands, you must complete two setup steps.

### Step 1: Configure Legacy Database Connection

```bash
./vars-legacy-migrator configure
```

This command prompts for:

| Prompt | Description |
|--------|-------------|
| JDBC URL | Connection string for the legacy SQL Server database (e.g., `jdbc:sqlserver://host:port;databaseName=VARS`) |
| Database Username | Your database username |
| Database Password | Your database password |

The connection is tested before saving. Credentials are encrypted and stored locally.

### Step 2: Login to Target Services

```bash
./vars-legacy-migrator login <razielServiceUrl>
```

This authenticates with the Raziel service to obtain credentials for VampireSquid and Annosaurus.

| Prompt | Description |
|--------|-------------|
| Username | Your MBARI username |
| Password | Your MBARI password |

Credentials are encrypted and stored locally for subsequent commands.

## Commands

### Check Service Health

Verify connectivity to the target services:

```bash
./vars-legacy-migrator service-health
```

Run this after setup to confirm everything is configured correctly.

### Migrate a Single Video Archive

Migrate a specific VideoArchiveSet by name:

```bash
./vars-legacy-migrator migrate-one <videoArchiveName> <csvLookupPath>
```

| Argument | Description |
|----------|-------------|
| `videoArchiveName` | The name of the VideoArchiveSet in the legacy database |
| `csvLookupPath` | Path to the CSV file containing camera ID mappings |

### Migrate All Video Archives

Migrate all VideoArchives from the legacy database:

```bash
./vars-legacy-migrator migrate-all <csvLookupPath>
```

| Argument | Description |
|----------|-------------|
| `csvLookupPath` | Path to the CSV file containing camera ID mappings |

## Workflow

A typical migration workflow:

1. **Setup**: Run `configure` and `login` commands
2. **Verify**: Run `service-health` to confirm connectivity
3. **Test**: Run `migrate-one` on a single archive to verify the process
4. **Migrate**: Run `migrate-all` to migrate all archives

## Troubleshooting

### Connection Failed

If `configure` reports a connection failure:

- Verify the JDBC URL format is correct
- Ensure network access to the SQL Server
- Confirm username and password are correct

### Login Failed

If `login` reports authentication failure:

- Verify the Raziel service URL is correct
- Confirm your MBARI credentials
- Check network access to the Raziel service

### Service Health Failures

If `service-health` reports issues:

- Re-run `login` to refresh credentials
- Verify network access to VampireSquid and Annosaurus
- Contact system administrators if services are down
