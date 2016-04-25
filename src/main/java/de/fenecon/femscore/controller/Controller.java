package de.fenecon.femscore.controller;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.ModbusWorker;

public class Controller extends Thread {
	private final static Logger log = LoggerFactory.getLogger(Controller.class);

	public final Collection<ModbusWorker> modbusWorkers;

	public Controller(String name, Collection<ModbusWorker> modbusWorkers) {
		setName(name);
		this.modbusWorkers = modbusWorkers;
	}

	@Override
	public void run() {
		log.info("Controller {} started", getName());
		for (ModbusWorker modbusWorker : modbusWorkers) {
			modbusWorker.start();
		}

		while (!isInterrupted()) {
			try {
				for (ModbusWorker modbusWorker : modbusWorkers) {
					modbusWorker.waitForMainQuery();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
				interrupt();
			}
			log.info("MainQueryFinished for all ModbusWorkers");
		}
		log.info("Controller {} stopped", getName());
	}

	@Override
	public String toString() {
		return "Controller [modbusWorkers=" + modbusWorkers + "]";
	}
}
