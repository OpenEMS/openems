package de.fenecon.femscore.controller;

import java.util.Map;

import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.ess.Ess;

public abstract class Controller {
	protected final Map<String, Ess> essDevices;
	protected final Map<String, Counter> counterDevices;

	public Controller(Map<String, Ess> essDevices, Map<String, Counter> counterDevices) {
		this.essDevices = essDevices;
		this.counterDevices = counterDevices;
	}

	public abstract void init();

	public abstract void run();
}
