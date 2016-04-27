package de.fenecon.femscore.controller;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.ModbusWorker;

public class ControllerWorker extends Thread {
	private final static Logger log = LoggerFactory.getLogger(ControllerWorker.class);

	private final Collection<ModbusWorker> modbusWorkers;
	private final Controller controller;

	public ControllerWorker(String name, Collection<ModbusWorker> modbusWorkers, Controller controller) {
		setName(name);
		this.modbusWorkers = modbusWorkers;
		this.controller = controller;
	}

	@Override
	public void run() {
		log.info("ControllerWorker {} started", getName());
		// Initialize ModbusWorkers
		for (ModbusWorker modbusWorker : modbusWorkers) {
			modbusWorker.start();
		}
		for (ModbusWorker modbusWorker : modbusWorkers) {
			try {
				modbusWorker.waitForInitQuery();
			} catch (InterruptedException e) {
				interrupt();
			}
		}
		controller.init();

		while (!isInterrupted()) {
			try {
				for (ModbusWorker modbusWorker : modbusWorkers) {
					modbusWorker.waitForMainQuery();
				}
				log.info("MainQueryFinished for all ModbusWorkers");
				controller.run();
			} catch (InterruptedException e) {
				interrupt();
			}
		}
		log.info("ControllerWorker {} stopped", getName());
	}

	@Override
	public String toString() {
		return "ControllerWorker [modbusWorkers=" + modbusWorkers + "]";
	}
}
