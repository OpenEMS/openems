package de.fenecon.femscore.config;

import java.util.HashMap;
import java.util.logging.Logger;

import de.fenecon.femscore.controller.ControllerWorker;
import de.fenecon.femscore.modbus.ModbusWorker;
import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.ess.Ess;
import de.fenecon.femscore.monitoring.MonitoringWorker;

public class Config {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Config.class.getName());

	private final String devicekey;
	private final HashMap<String, ModbusWorker> modbuss;
	private final HashMap<String, Ess> esss;
	private final HashMap<String, Counter> counters;
	private final HashMap<String, ControllerWorker> controllers;
	private final HashMap<String, MonitoringWorker> monitorings;

	public Config(String devicekey, HashMap<String, ModbusWorker> modbuss, HashMap<String, Ess> esss,
			HashMap<String, Counter> counters, HashMap<String, ControllerWorker> controllers,
			HashMap<String, MonitoringWorker> monitorings) {
		this.devicekey = devicekey;
		this.modbuss = modbuss;
		this.esss = esss;
		this.counters = counters;
		this.controllers = controllers;
		this.monitorings = monitorings;
	}

	public String getDevicekey() {
		return devicekey;
	}

	public HashMap<String, ModbusWorker> getModbuss() {
		return modbuss;
	}

	public HashMap<String, Ess> getEsss() {
		return esss;
	}

	public HashMap<String, Counter> getCounters() {
		return counters;
	}

	public HashMap<String, ControllerWorker> getControllers() {
		return controllers;
	}

	public HashMap<String, MonitoringWorker> getMonitorings() {
		return monitorings;
	}

	@Override
	public String toString() {
		return "Config [modbusWorkers=" + modbuss + ", esss=" + esss + ", counters=" + counters + ", controllerWorkers="
				+ controllers + "]";
	}
}