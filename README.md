# SqlRelMan

**SQL Releases Management** — a lightweight, developer-first CLI tool for organizing SQL migrations into structured releases, tracking what has been applied, and rolling back cleanly when things go wrong.

---

## What is SqlRelMan?

Database migrations tend to sprawl. Scripts accumulate in folders with names like `fix_final_v2_REAL.sql`, applied manually, out of order, on environments that no longer match each other. SqlRelMan brings structure to that chaos.

The core idea is simple: SQL changes are grouped into **patches**, patches are grouped into **releases**, and SqlRelMan tracks which patches have been applied to which database. Run `migrate`, get repeatable, ordered execution. Run `rollback`, undo exactly one patch. Every action is traceable, every state is inspectable.

---

## Features

- **Releases & patches** — organize migrations into versioned releases, each containing ordered, named patches with `up.sql` and `down.sql` scripts
- **Migration tracking** — a `sqlrelman_migrations` table records every applied patch; re-running `migrate` skips what's already there
- **Rollback** — roll back a specific patch by name or use `--last` to undo the most recently applied one
- **Interactive shell** — a full REPL with tab completion, context namespaces (`patch>`, `release>`) and command history
- **Built-in SQL editor** — edit migration scripts directly in the terminal without needing a system editor configured
- **Multi-database support** — MySQL, PostgreSQL, MariaDB, SQL Server, Oracle, SQLite, H2
- **Driver management** — JDBC drivers are downloaded from Maven Central on demand and cached locally; no classpath configuration required
- **Setup wizard** — `setup` walks through database type, URL, credentials, driver download, and connection test in one flow
- **Self-contained builds** — releases ship with a bundled OpenJDK 25 so no Java installation is required on the target machine

---

## Getting Started

### Prerequisites (slim builds)

- Java 17 or newer — check with `java -version`

Bundled builds include OpenJDK 25 and have no external dependencies.

### Installation

Download the appropriate archive for your platform from the [Releases](../../releases) page.

**Linux / macOS**

```sh
# Extract
tar -xzf sqlrelman-*-linux-bundled-jre25.tar.gz   # or -macos-arm64- / -macos-x64-

# Run immediately from the extracted folder
./sqlrelman-*/bin/sqlrelman

# Or install to ~/.local/bin (adds it to PATH)
cd sqlrelman-*/
./install.sh
```

> **macOS Gatekeeper:** on first run macOS may block the binary. Clear the quarantine flag with:
> ```sh
> xattr -dr com.apple.quarantine sqlrelman-*/
> ```

**Windows**

1. Extract the zip
2. Double-click `install.bat` — installs to `%USERPROFILE%\.local\bin`
3. Or run `bin\sqlrelman.bat` directly from the extracted folder

**Any platform (raw JAR)**

```sh
java -jar sqlrelman-*.jar
```

### Build from source

```sh
git clone https://github.com/serbantudor04/sqlrelman.git
cd sqlrelman
./gradlew fatJar
java -jar build/libs/sqlrelman-*.jar
```

Install to `~/.local/bin`:

```sh
./gradlew install
```

---

## Usage

SqlRelMan works both as a direct CLI and as an interactive shell.

### Interactive shell

Launching with no arguments drops into the REPL:

```
sqlrelman
```

```
  ┌─────────────────────────────────────────┐
  │        SQLRelMan Interactive Shell       │
  │                  v1.0                    │
  └─────────────────────────────────────────┘
sqlrelman>
```

Type `patch` or `release` to enter a context namespace. Commands inside that namespace omit the prefix — so in the `patch>` context, `list 1.0.0` is equivalent to `patch list 1.0.0` at the root.

### Direct CLI

```sh
sqlrelman <command> [args...]
```

---

## Commands

### `setup`

Interactive wizard — configures the database, downloads the JDBC driver, and tests the connection.

```sh
sqlrelman setup
```

### `release`

Manage release directories.

```sh
release create <version> [description]   # create a new release
release list                             # list all releases
release info <version>                   # show patches and metadata
release delete <version>                 # delete a release and all its patches
```

### `patch`

Manage patches within a release.

```sh
patch create <release> <name> [description]       # add a patch
patch list   <release>                            # list patches in a release
patch show   <release> <patch-id> [up|down]       # print a SQL script
patch edit   <release> <patch-id> [up|down|both]  # open in editor
patch delete <release> <patch-id>                 # remove a patch
```

### `migrate`

Apply all pending patches in a release, in order.

```sh
sqlrelman migrate <release>
```

Already-applied patches are skipped. Progress is printed for every patch.

### `rollback`

Run the `down.sql` of a patch and remove its migration record.

```sh
sqlrelman rollback <release> <patch-id>   # roll back a specific patch
sqlrelman rollback <release> --last       # roll back the most recently applied patch
```

### `ls`

Tree view of releases and patches.

```sh
sqlrelman ls            # all releases
sqlrelman ls <version>  # one release with patch descriptions
```

### `driver`

Manage JDBC drivers.

```sh
driver list                         # show all supported drivers and cache status
driver download <type> [--force]    # download a driver JAR from Maven Central
driver info <type>                  # show driver coordinates and cached path
```

Supported types: `mysql`, `postgresql`, `mariadb`, `sqlserver`, `oracle`, `sqlite`, `h2`

### `help`

List all available commands.

```sh
sqlrelman help
```

---

## Directory Layout

SqlRelMan stores everything under a configurable `releases/` directory (default: `./releases`). The structure is straightforward:

```
releases/
└── 1.0.0/
│   ├── info.json              ← release metadata and patch list
│   ├── 001_create_users/
│   │   ├── up.sql
│   │   └── down.sql
│   └── 002_add_email_index/
│       ├── up.sql
│       └── down.sql
└── 1.1.0/
    ├── info.json
    └── 001_add_audit_table/
        ├── up.sql
        └── down.sql
```

Configuration and cached drivers live in `~/.sqlrelman/`.

---

## Configuration

`setup` writes all settings to `~/.sqlrelman/config.properties`. You can also edit it directly:

```properties
releases.dir=./releases
db.type=postgresql
db.url=jdbc:postgresql://localhost:5432/mydb
db.user=alice
db.password=secret
# db.driver=   (optional override — leave blank to use the default for db.type)
```

---

## Roadmap

SqlRelMan is currently a standalone CLI. The following are planned for future releases:

### Server mode
A long-running server process that manages a shared migration state, allowing multiple developers and CI pipelines to coordinate against the same database without stepping on each other. Planned capabilities include environment locking, audit logging, and a central view of which release is applied where.

### Client / server architecture
A thin client that speaks to the SqlRelMan server, enabling teams to run migrations remotely without direct database credentials on every developer machine. The standalone CLI will continue to work independently.

### Web interface
A browser-based dashboard for browsing releases, viewing migration history, triggering runs, and inspecting patch content — aimed at teams that prefer a visual workflow alongside the CLI.

### IDE integration
Editor plugins (initially targeting IntelliJ IDEA) for creating and editing patches, running migrations, and viewing applied state without leaving the development environment.

---

## License

MIT — see [LICENSE](LICENSE) for details.

---

## Author

Serban Tudor — [github.com/serbantudor04](https://github.com/serbantudor04)