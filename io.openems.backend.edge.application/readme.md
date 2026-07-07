# OpenEMS BackendEdge App

## Overview

The OpenEMS BackendEdge App is a proxy application that acts as an intermediary between OpenEMS Edge instances and the
OpenEMS Backend. It runs on an independent (Linux) server and serves as a communication bridge, allowing multiple Edge
Controller API clients to connect to a single Backend through a dedicated application server.

## Purpose

- **Acts as a proxy/relay** between OpenEMS Edges and the OpenEMS Backend
- **Bidirectional WebSocket communication** for real-time data exchange
- **Scalable architecture** that can handle multiple Edge connections
- **Stateful caching** to maintain connection state and coordinate communication

## Architecture

```
OpenEMS Edges
      ↓
  (WebSocket on port 8081)
      ↓
[BackendEdgeApp Server] ← Relays requests/responses → [BackendEdgeApp Client]
                                                              ↓
                                              (WebSocket to Backend Edge-Manager)
                                                          ↓
                                          [OpenEMS Backend Edge-Manager]
      ↓
   Cache (Stores shared state)
```

## Key Components

### 1. WebSocket Server

- **Purpose**: Accepts incoming connections from OpenEMS Edge devices
- **Default Port**: 8081 (configurable)
- **Thread Pool Size**: 10 threads (configurable)
- **Configuration**: `port` parameter in Felix console

### 2. WebSocket Client

- **Purpose**: Connects to the OpenEMS Backend Edge-Manager
- **Default URI**: `ws://localhost:8083` (configurable)
- **Thread Pool Size**: 10 threads (configurable)
- **Configuration**: `uri` parameter in Felix console

## Configuration

The application is configured via the **Felix Web Console** at `http://localhost:8078`

### Configuration Parameters

| Parameter          | Description                                           | Default               |
|--------------------|-------------------------------------------------------|-----------------------|
| **id**             | Unique ID of this Backend Edge Application            | `edges0`              |
| **uri**            | Connection URI to Backend Edge-Manager                | `ws://localhost:8083` |
| **clientPoolSize** | Number of threads dedicated to handle Client tasks    | 10                    |
| **port**           | WebSocket server port for Edge Controller connections | 8081                  |
| **serverPoolSize** | Number of threads dedicated to handle Server tasks    | 10                    |

## Felix Web Console

The BackendEdgeApp includes the Apache Felix Web Console for administration and monitoring:

- **URL**: `http://localhost:8078`
- **HTTP Port**: 8078 (configurable via `org.osgi.service.http.port` in BackendEdgeApp.bndrun)
- **Configuration Directory**: `c:/openems-backend-edge/config`
- **Included Plugins**:
  - Felix Web Console
  - Declarative Services (DS) plugin
  - Inventory plugin
  - Configuration Admin

## Building the Application

### Using Gradle

The dedicated Gradle task `buildBackendEdgeApp` is available in the root `build.gradle`:

```bash
./gradlew buildBackendEdgeApp
```

This task:

1. Assembles all Backend bundles via `assembleBackendEdge` task
2. Resolves dependencies using `resolve.BackendEdgeApp`
3. Exports the application using `export.BackendEdgeApp`
4. Generates the final JAR file

**Output**: `build/openems-backend-edge.jar`

### Environment Variables

You can override the output location:

```bash
# Set custom output path (Unix/Linux)
export OEMS_BACKEND_EDGE_OUTPUT=/path/to/custom-backend-edge.jar
./gradlew buildBackendEdgeApp

# Or use Gradle property
./gradlew buildBackendEdgeApp -Poems.backend.edge.output=/path/to/custom-backend-edge.jar
```

### Using Bnd Tasks Directly

If you prefer using Bnd's native tasks:

```bash
# Step 1: Resolve dependencies (updates BackendEdgeApp.bndrun)
./gradlew resolve.BackendEdgeApp

# Step 2: Export as executable JAR
./gradlew export.BackendEdgeApp
```

**Output**: `io.openems.backend.edge.application/generated/distributions/executable/BackendEdgeApp.jar`

## Running the Application

After building the JAR:

```bash
java -jar openems-backend-edge.jar
```

The application will:

1. Listen for OpenEMS Edge connections on port 8081
2. Connect to Backend Edge-Manager at the configured URI
3. Proxy requests and responses between edges and backend
4. Expose Felix Web Console on port 8078
5. Log to configured logging backend (Log4j2)

## Development

### Module Location

- **Module**: `io.openems.backend.edge.application`
- **Package**: `io.openems.backend.edge.application`
- **Main Class**: `BackendEdgeServerApp`

### Configuration Class

- **Class**: `Config.java`
- **Type**: OSGi Configuration Admin (Metatype)
- **Designation**: Factory = false (single instance)

### Related Modules

- `io.openems.backend.edge.manager` - Edge management backend service
- `io.openems.backend.common` - Shared backend utilities
- `io.openems.backend.application` - Main Backend application
