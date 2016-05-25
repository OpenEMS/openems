package de.fenecon.femscore;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.config.Config;
import de.fenecon.femscore.config.JsonConfigFactory;
import de.fenecon.femscore.controller.ControllerWorker;
import de.fenecon.femscore.modbus.device.ModbusDevice;

/**
 * Main App
 *
 */
public class App {
	private final static Logger log = LoggerFactory.getLogger(ModbusDevice.class);

	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = JsonConfigFactory.readConfigFromJsonFile();
		log.info("Config: " + config);
		for (ControllerWorker controllerWorker : config.getControllerWorkers().values()) {
			controllerWorker.start();
		}
	}
}
