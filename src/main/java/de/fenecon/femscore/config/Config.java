package de.fenecon.femscore.config;

import java.util.HashMap;
import java.util.logging.Logger;

import de.fenecon.femscore.controller.ControllerWorker;
import de.fenecon.femscore.modbus.ModbusWorker;
import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.ess.Ess;

public class Config {
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Config.class.getName());

	private final HashMap<String, ModbusWorker> modbusWorkers;
	private final HashMap<String, Ess> esss;
	private final HashMap<String, Counter> counters;
	private final HashMap<String, ControllerWorker> controllerWorkers;

	public Config(HashMap<String, ModbusWorker> modbusWorkers, HashMap<String, Ess> esss,
			HashMap<String, Counter> counters, HashMap<String, ControllerWorker> controllerWorkers) {
		this.modbusWorkers = modbusWorkers;
		this.esss = esss;
		this.counters = counters;
		this.controllerWorkers = controllerWorkers;
	}

	public HashMap<String, ModbusWorker> getModbusWorkers() {
		return modbusWorkers;
	}

	public HashMap<String, Ess> getEsss() {
		return esss;
	}

	public HashMap<String, Counter> getCounters() {
		return counters;
	}

	public HashMap<String, ControllerWorker> getControllerWorkers() {
		return controllerWorkers;
	}

	@Override
	public String toString() {
		return "Config [modbusWorkers=" + modbusWorkers + ", esss=" + esss + ", counters=" + counters
				+ ", controllerWorkers=" + controllerWorkers + "]";
	}
}