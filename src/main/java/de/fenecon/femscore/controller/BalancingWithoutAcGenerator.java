package de.fenecon.femscore.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.device.counter.Counter;
import de.fenecon.femscore.modbus.device.ess.Ess;

public class BalancingWithoutAcGenerator extends Balancing {
	private final static Logger log = LoggerFactory.getLogger(BalancingWithoutAcGenerator.class);

	public BalancingWithoutAcGenerator(String name, Map<String, Ess> essDevices, Map<String, Counter> counterDevices) {
		super(name, essDevices, counterDevices, false);
	}
}
