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
