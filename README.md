# OpenEMS
## Open Source Energy Management System

OpenEMS is a modular platform for energy management applications. It was developed around the requirements of controlling, monitoring and integrating energy storage systems together with renewable energy sources and further devices and services.

#### OpenEMS IoT stack
The OpenEMS stack contains three parts:
 * **OpenEMS Edge**: this runs on the IoT edge device and actually controls the devices
 * **OpenEMS UI**: the generic user interface
 * **OpenEMS Backend**: this runs on a (cloud) server, connects the decentralized Edge systems and provides monitoring and control via internet

#### Features:

 * Fast, PLC-like control of battery inverters and other devices
 * Easily extendable due to the use of modern programming languages and modular architecture
 * Wide range of supported devices - (battery) inverters, meters, etc. - and protocols
 * Modern web-based real-time user interface

#### Screenshots
<img src="/doc/img/screenshots/overview.png" width="100" align="left"/>
<img src="/doc/img/screenshots/energymonitor.png" width="100" align="left"/>
<img src="/doc/img/screenshots/energytable.png" width="100" align="left"/>
<img src="/doc/img/screenshots/briefhistorie.png" width="100" align="left"/>
<img src="/doc/img/screenshots/historysoc.png" width="100" align="left"/>
<img src="/doc/img/screenshots/historyenergy.png" width="100"/>

## Get started

The target of this short guide is to quickly setup a development environment on your local machine. First you are going to setup OpenEMS Edge to simulate an energy storage system. The second part is setting up the OpenEMS UI.

#### Get the source code

1. Download a [git client](https://git-scm.com/) and install it.
2. Open a terminal, change to your preferred directory and clone the repository:

	* On Windows: Press `Windows` + `R`, enter `cmd` and press `Enter`.
	* `git clone https://github.com/OpenEMS/openems.git`

### Setup OpenEMS simulator

1. Download [Eclipse for Java](https://www.eclipse.org/), install and start it
2. Import the Edge project in Eclipse:

	Menu: `File` →  `Import...` → `General` → `Existing Projects into Workspace` → 
    Select root directory: `Browse...` → Select the "openems" directory → `OK` → `Finish`
    
3. Apply the simulator template configuration.
	Hint: OpenEMS is using a global JSON configuration file.
    
    Copy `template/Simulator.json` to `etc/openems.d/config.json`


4. Right click on `src/io.openems/App.java` and select `Run As...` → `Java Application`.

5. You should see it running in the console.

### Setup OpenEMS UI

1. Download [node.js LTS](https://nodejs.org) and install it.
2. Download [Visual Studio Code](https://code.visualstudio.com/), install and start it.
3. Open UI project in Visual Studio Code:

	Menu: `File` → `Open directory...` → Select the "openems/ui/" directory → `Select directory`

4. Open the integrated terminal:

	Menu: `Show` → `Integrated terminal`

5. Install [Angular CLI](https://cli.angular.io/):

	`npm install -g @angular/cli`
    
6. Install dependencies:

	`npm install`
    
7. Run OpenEMS UI:

	`ng serve`
    
8. Open a browser at http://localhost:4200


## Professional services and support

OpenEMS is mainly developed by [FENECON GmbH](https://www.fenecon.de) in Germany. We are specialized in manufacturing and project development of energy storage systems. If you are interested in OpenEMS our development team would be glad to hear from you: fems@fenecon.de.
