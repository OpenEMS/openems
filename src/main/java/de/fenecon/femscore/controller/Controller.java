package de.fenecon.femscore.controller;

import java.util.HashMap;
import java.util.Map;

import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.ess.Ess;

public abstract class Controller {
	private final String name;
	protected final Map<String, Ess> esss;
	protected final Map<String, Counter> counters;

	public Controller(String name, Map<String, Ess> esss, Map<String, Counter> counters) {
		this.name = name;
		if (esss == null) {
			this.esss = new HashMap<String, Ess>();
		} else {
			this.esss = esss;
		}
		if (counters == null) {
			this.counters = new HashMap<String, Counter>();
		} else {
			this.counters = counters;
		}
	}

	public abstract void init();

	public abstract void run();
}
