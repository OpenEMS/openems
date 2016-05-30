package de.fenecon.femscore.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.ess.Ess;

public class BalancingWithAcGenerator extends Balancing {
	private final static Logger log = LoggerFactory.getLogger(BalancingWithAcGenerator.class);

	public BalancingWithAcGenerator(String name, Map<String, Ess> essDevices, Map<String, Counter> counterDevices) {
		super(name, essDevices, counterDevices, true);
	}
}
