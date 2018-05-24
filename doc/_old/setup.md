# Setup development environment

## OpenEMS

### Clone OpenEMS from Github:
```
git clone https://github.com/OpenEMS/openems.git
```

### Configure IDE

Open Eclipse IDE/IntelliJ and import project

### Prepare configuration

copy /template/Simulator.json to /etc/openems.d/config.json

### Run OpenEMS

Find /src/io.openems/App.java, right click and "Run".


### Setup InfluxDB (if you want to store the data)

- Download InfluxDB (influxdata.com)
- Extract zip file
- start "influxd.exe" and stop it again. This created the config file "influxdb.conf"
- Create "start.bat":
	influxd.exe -config influxdb.conf
- You can enable InfluxDB web interface by setting "enabled = true" in section "[admin]"
- Configure OpenEMS to use InfluxDB: add in config.json after closing bracket of "scheduler" the following and then restart OpenEMS:
	"persistence": [
		{
			"class": "io.openems.impl.persistence.influxdb.InfluxdbPersistence",
			"ip": "127.0.0.1",
			"fems": 1 
		}
	]
- execute "start.bat"

## OpenEMS-UI

### Clone OpenEMS-UI from Github
```
git clone https://github.com/OpenEMS/openems-gui.git
```

### Install applications
- nodejs (https://nodejs.org/en/)
- angular-cli (https://github.com/angular/angular-cli#installation)

### Install dependencies
Change to openems-gui folder and execute
```
npm install
```

### Run OpenEMS-UI
```
ng serve
```
Open Browser at http://localhost:4200