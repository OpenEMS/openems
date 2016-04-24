package de.fenecon.femscore.controller;

import java.util.Collection;

import de.fenecon.femscore.modbus.ModbusWorker;

public class Controller {
	public final Collection<ModbusWorker> modbusWorkers;
	
	public Controller(Collection<ModbusWorker> modbusWorkers) {
		this.modbusWorkers = modbusWorkers; 
	}
	
	public void start() {
		for(ModbusWorker modbusWorker : modbusWorkers) {
			modbusWorker.start();
		}
	}

	@Override
	public String toString() {
		return "Controller [modbusWorkers=" + modbusWorkers + "]";
	}
}
