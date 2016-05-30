package de.fenecon.femscore.controller;

import java.util.Map;

import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.ess.Ess;

public class BalancingWithAcGenerator extends Balancing {
	public BalancingWithAcGenerator(String name, Map<String, Ess> essDevices, Map<String, Counter> counterDevices) {
		super(name, essDevices, counterDevices, true);
	}
}
