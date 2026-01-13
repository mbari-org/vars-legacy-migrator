# vars-legacy-migrator

A Scala 3 CLI tool for migrating first generation [VARS databases](https://github.com/hohonuuli/vars) to the [current infrastructure](https://github.com/mbari-org/m3-quickstart). This is proprietary internal software for MBARI.

## Quick Links

- [Documentation](docs/index.md) - Full documentation
- [User Guide](docs/users/index.md) - Installation and usage
- [Developer Guide](docs/development/developers.md) - Building and architecture
- [Scala API](api/index.html) - API reference
- [GitHub](https://github.com/mbari-org/vars-legacy-migrator) - Source code

## Overview

This tool migrates legacy VARS annotation data from SQL Server to modern microservices:

1. **Legacy Source**: Reads from SQL Server database (VARS legacy)
2. **Transformation**: Converts VideoArchives and Observations to Media and Annotations
3. **Target Services**: Writes to VampireSquid and Annosaurus
