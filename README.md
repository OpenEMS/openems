# OpenEMS
## Open Source Energy Management System

OpenEMS is a modular platform for energy management applications. It was developed around the requirements of controlling, monitoring and integrating energy storage systems together with renewable energy sources and further devices and services.

_Note: Improvement of this documentation is currently in progress. Please find the latest draft version here:_ https://github.com/OpenEMS/openems/blob/feature/improve_doc/doc/openems.adoc

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

## Compatibility

A number of devices, protocols and services are already implemented in OpenEMS:

### Integrated energy storage sytems / inverters / battery chargers

 * [FENECON Mini 3-3 and 3-6 (testing)](https://fenecon.de/page/stromspeicher-mini-es)
 * [FENECON Pro 9-12](https://fenecon.de/page/stromspeicher-pro)
 * [FENECON Commercial AC](https://fenecon.de/page/stromspeicher-commercial)
 * [FENECON Commercial DC](https://fenecon.de/page/stromspeicher-commercial)
 * [REFU battery inverter](http://www.refu-energy.de/hybrid-power-applications/)
 * [Studer VS 70 (testing)](http://www.studer-innotec.com/de/produkte/variostring-reihe/vs-70-313)

### Meters

 * [SOCOMEC Diris](http://www.socomec.de/multimessgerate_de.html)
 * [B-Control Energy Meter](https://www.b-control.com)
 * [Janitza UMG 96RM-E](https://www.janitza.de/umg-96rm-e.html)
 * [Carlog Gavazzi EM300](http://www.gavazzi.de/index.php/13-control/47-em100-und-em300-serie)
 * [PQ plus UMD 97](http://www.pq-plus.de/news/pqplus/umd-97-messgeraet.html)

### Input/Output

 * [FEMS Relais 8-Kanal RS485](https://fenecon.de/page/stromspeicher-fems)
 * [FEMS Relais 8-Kanal TCP](https://fenecon.de/page/stromspeicher-fems)
 * [WAGO Fieldbus](http://www.wago.de/produkte/produktkatalog/automatisierungskomponenten/modulares-wago-io-system-ip20-serie-750-753/feldbuskoppler/index.jsp)
 * [KMTronic relay board](https://www.kmtronic.com/)

### Electric Vehicle Charging Stations (EVCS)

 * [KEBA KeContact P30 (testing)](http://www.keba.com/de/emobility/elektromobilitaet)

### Data persistence / logging / monitoring

 * [InfluxDB](https://www.influxdata.com)

### Outbound Protocols

 * Modbus/TCP
 * Modbus/RTU

### Inbound protocols

 * [JSON/REST](doc/rest-api.md)
 * JSON/Websocket
 * [Modbus/TCP](/doc/modbustcp-api.md)
 
### Technical

 * [OpenEMS Edge Software Architecture](doc/architecture.md)
 * [Data abstraction: OpenEMS Channels](doc/channels.md)

## Get started

The target of this short guide is to quickly setup a development environment on your local machine. First you are going to setup OpenEMS Edge to simulate an energy storage system. The second part is setting up the OpenEMS UI.

#### Get the source code

1. Download a [git client](https://git-scm.com/) and install it.
2. Open a terminal, change to your preferred directory and clone the repository:

	* On Windows: Press `Windows` + `R`, enter `cmd` and press `Enter`.
	* `git clone https://github.com/OpenEMS/openems.git`

### Setup OpenEMS simulator

1. Download [Eclipse for Java](https://www.eclipse.org/), install and start it
2. Install [BndTools](http://bndtools.org/) in Eclipse:

	Menu: `Help` →  `Eclipse Marketplace...` → `Find:` → enter `BndTools` → press `Install`

3. Import OSGi projects:

	Menu: `File` →  `Import...` → `Bndtools` → `Existing Bnd Workspace` → Root directory: `Browse...` → select the directory with the source code → `OK` → `Finish` → "Switch to Bndtools perspective?" `yes`

	If Eclipse shows errors: Because of a bug in Bndtools it is necessary to manually trigger a build. Doubleclick the `cnf` project → doubleclick `build.bnd` → click on `Reload` in bottom right of the window. The errors should all disappear.

4. Import Java projects:

    Menu: `File` →  `Import...` → `General` → `Existing Projects into Workspace` → "Select root directory:" `Browse...` → select the directory with the source code → `Finish`

5. Apply the simulator template configuration.
	Hint: OpenEMS is using a global JSON configuration file.
    
    Open the `openems` project and copy `template/Simulator.json` to `etc/openems.d/config.json`

6. Right click on `src/io.openems/App.java` and select `Run As...` → `Java Application`.

7. You should see it running in the console.

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

### How to configure OpenEMS

The configuration of OpenEMS is placed in the json file /etc/openems.d/config.json.
To parameterize a Bridge,Device,DeviceNature, Scheduler, Controller or Persistance you have to use ConfigChannels. A ConfigChannel represents one parameter the Thing needs. The value of the ConfigChannel will be automatically parsed by OpenEMS on the configuration read. Each thing will be instantiated by reflection so you have to define a "class" property, which is the qualified name of the desired class.
The file is split into three sections:
1. `things`:
	In the things section you need to set all devices to communicate with.
    Therefore you have to use a so called "Bridge". A Bridge connects several devices(Hardware) with the same protocol to openems. For example you want to read the data of a Socomec Meter and FeneconPro which are connected to the same RS485 Bus, you have to use a ModbusRtu bridge and set the Socomec Meter and FeneconPro as devices for this bridge. Each bridge/device has ConfigChannels to provide the paremters to establish the connection. The required parameters can be found in the according class. A device has DeviceNatures, where each nature requires a unique id. This id is used as reference by the controllers.
2. `scheduler`: The Scheduler executes the controllers according to the programmed behaviour. For example the SimpleScheduler orders and executes the Controller by the configured priority. Controllers are the smallest divisible logical control unit. Each Controller needs at least one device reference to do the work.
3. `persistence`:

Example configuration:
```
{
	"things": [
		{
			"class": "io.openems.impl.protocol.modbus.ModbusRtu",
			"serialinterface": "/dev/ttyUSB0",
			"baudrate": 9600,
			"databits": 8,
			"parity": "none",
			"stopbits": 1,
			"devices": [
				{
					"class": "io.openems.impl.device.pro.FeneconPro",
					"modbusUnitId": 4,
					"ess": {
						"id": "ess0",
						"minSoc": 15
					},
					"meter": {
						"id": "meter1"
					}
				},
				{
					"class": "io.openems.impl.device.socomec.Socomec",
					"modbusUnitId": 5,
					"meter": {
						"id": "meter0",
						"type": "grid"
					}
				}
			]
		}
	],
	"scheduler": {
		"class": "io.openems.impl.scheduler.SimpleScheduler",
		"controllers": [
			{
				"priority": 150,
				"class": "io.openems.impl.controller.debuglog.DebugLogController",
				"esss": [ "ess0" ],
				"meters": [ "meter0", "meter1" ],
				"rtc": "ess0"
			},
			{
				"priority": 100,
				"class": "io.openems.impl.controller.asymmetric.avoidtotaldischarge.AvoidTotalDischargeController",
				"esss": "ess0"
			},
			{
				"priority": 50,
				"class": "io.openems.impl.controller.asymmetric.balancing.BalancingController",
				"esss": "ess0",
				"meter": "meter0"
			},
			{
				"priority": 1,
				"class": "io.openems.impl.controller.clocksync.ClockSyncController",
				"rtc": "ess0"
			},
			{
				"priority": 0,
				"class": "io.openems.impl.controller.feneconprosetup.FeneconProSetupController",
				"esss": "ess0"
			}
		]
	},
	"persistence": [
		{
			"class": "io.openems.impl.persistence.influxdb.InfluxdbPersistence",
			"ip": "127.0.0.1",
			"fems": 0
		}
	]	
}
```

## Professional services and support

OpenEMS is mainly developed by [FENECON GmbH](https://www.fenecon.de) in Germany. We are specialized in manufacturing and project development of energy storage systems. If you are interested in OpenEMS our development team would be glad to hear from you: fems@fenecon.de.
