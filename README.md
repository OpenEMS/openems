[![Build Status](https://github.com/veloce-argos/openems/actions/workflows/build.yml/badge.svg)](https://github.com/veloce-argos/openems/actions/workflows/build.yml)

<h1 align="center">
  <img src="./doc/modules/ROOT/assets/images/OpenEMS-Logo.png" alt="the Feneco - OpenEMS Logo" width="50"></a>
  <br/>Open Source Energy Management System
</h1>

OpenEMS - the Open Source Energy Management System - is a modular platform for energy management applications. It was developed around the requirements of monitoring, controlling, and integrating energy storage together with renewable energy sources and complementary devices and services like electric vehicle charging stations, heat-pumps, electrolysers, time-of-use electricity tariffs and more.

This is a fork of the [OpenEMS](https://github.com/OpenEMS/openems) project.  Community contributions & bug fixes by Veloce Energy developers will be branched from and merged back into the **branch** branch.  Features relevant only for Veloce Energy will be branched from and merged back into to the **veloce** branch. 

## OpenEMS IoT stack

The OpenEMS 'Internet of Things' stack contains three main components:

 * **OpenEMS Edge** runs on site, communicates with devices and services, collects data and executes control algorithms
 * **OpenEMS UI** is the real-time user interface for web browsers and smartphones
 * **OpenEMS Backend** runs on a (cloud) server, connects the decentralized Edge systems and provides aggregation, monitoring and control via internet

## Features

The OpenEMS software architecture was designed to leverage some features that are required by a modern and flexible Energy Management System:

 * Fast, PLC-like control of devices
 * Easily extendable due to the use of modern programming languages and modular architecture
 * Reusable, device independent control algorithms due to clear device abstraction
 * Wide range of supported devices and protocols

 ## Build Tasks

To view build tasks, in a terminal on Mac, Linux, or Windows Subsystem for Linux:

 ```bash
./gradlew tasks
 ```

## Development

### Pre-requisites

- Java 17 - [Official](https://www.oracle.com/java/) or [OpenJDK](https://openjdk.org/)
- [Node.JS 16](https://nodejs.org/e)

Installing Java 17 on a Mac, Linux, or Windows Subsystem for Linux:

```bash
sudo bash -c "apt install openjdk-17-jdk -y"
echo '
JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64/bin/java"
export PATH=$JAVA_HOME"/bin":$PATH' >> ~/.bashrc

source ~/.bashrc  # refreshes the terminal session with JAVA_HOME set
```

Install NodeJS 16 using [NVM](https://github.com/nvm-sh/nvm):

```bash
# Configure NVM (Node Version Manager) and install the latest LTS (Long-Term Support) version of Node.js using NVM
wget -qO- https://raw.githubusercontent.com/nvm-sh/nvm/v0.35.3/install.sh | bash
echo 'export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"  # This loads nvm
[ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"' >> ~/.bashrc

source ~/.bashrc          # reset the session with NVM configuration changes

nvm install lts/gallium   # install Node LTS

# Install Angular (https://angular.io/) CLI globally using npm
npm i -g @angular/cli
```

### Getting Started

To build & run Edge application locally:

```bash
# Build the projects and the Edge application. This will take a few minuts
./gradlew build && ./gradlew buildEdge

# Configure the implementation on your system
sudo mkdir /usr/lib/openems && sudo mkdir /etc/openems.d
sudo cp build/openems-edge.jar /usr/lib/openems/openems.jar

# Run the Edge application
java -Dfelix.cm.dir=/etc/openems.d/ -jar /usr/lib/openems/openems.jar
```

To run the UI application locally:

```bash
cd ui && npm i
ng serve -o -c openems-edge-dev
```

If running on a development server and want to expose the UI on your LAN, add the `--host 0.0.0.0` flag.  For example:  `ng serve -o -c openems-edge-dev --host 0.0.0.0`.

## Documentation

* [Latest version of documentation](https://openems.github.io/openems.io/openems/latest/introduction.html)
* [Javadoc](https://openems.github.io/openems.io/javadoc/)

## License

* OpenEMS Edge 
* OpenEMS Backend

Copyright (C) 2016-2022 OpenEMS Association e.V.

This product includes software developed at FENECON GmbH: you can
redistribute it and/or modify it under the terms of the [Eclipse Public License version 2.0](LICENSE-EPL-2.0). 

 * OpenEMS UI

Copyright (C) 2016-2022 OpenEMS Association e.V.

This product includes software developed at FENECON GmbH: you can
redistribute it and/or modify it under the terms of the [GNU Affero General Public License version 3](LICENSE-AGPL-3.0).
