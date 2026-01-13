# vars-legacy-migrator

![MBARI Logo](images/logo-mbari-3b.png)

A Scala 3 CLI tool for migrating first generation [VARS databases](https://github.com/hohonuuli/vars) to the [current infrastructure](https://github.com/mbari-org/m3-quickstart). This is proprietary internal software for MBARI (Monterey Bay Aquarium Research Institute).

[GitHub](https://github.com/mbari-org/vars-legacy-migrator)

## Overview

This tool migrates legacy VARS annotation data from SQL Server to modern microservices:

1. **Legacy Source**: Reads VideoArchives and Observations from the legacy SQL Server database
2. **Transformation**: Converts legacy data structures to modern Media and Annotation formats
3. **Target Services**: Writes to [VampireSquid](https://github.com/mbari-org/vampire-squid) (media metadata) and [Annosaurus](https://github.com/mbari-org/annosaurus) (annotations)

## Documentation

- [User Guide](users/index.md) - Installation, setup, and usage instructions
- [Developer Guide](development/developers.md) - Building, architecture, and contributing
