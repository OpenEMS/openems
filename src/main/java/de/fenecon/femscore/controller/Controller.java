package de.fenecon.femscore.controller;

import java.util.HashMap;
import java.util.Map;

import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.ess.Ess;

public abstract class Controller {
	protected final Map<String, Ess> essDevices;
	protected final Map<String, Counter> counterDevices;

	public Controller(Map<String, Ess> essDevices, Map<String, Counter> counterDevices) {
		if (essDevices == null) {
			this.essDevices = new HashMap<String, Ess>();
		} else {
			this.essDevices = essDevices;
		}
		if (counterDevices == null) {
			this.counterDevices = new HashMap<String, Counter>();
		} else {
			this.counterDevices = counterDevices;
		}
	}

	public abstract void init();

	public abstract void run();
}
