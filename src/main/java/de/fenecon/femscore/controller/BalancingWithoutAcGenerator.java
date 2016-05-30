package de.fenecon.femscore.controller;

import java.util.Map;

import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.ess.Ess;

public class BalancingWithoutAcGenerator extends Balancing {
	public BalancingWithoutAcGenerator(String name, Map<String, Ess> essDevices, Map<String, Counter> counterDevices) {
		super(name, essDevices, counterDevices, false);
	}
}
