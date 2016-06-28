package de.fenecon.femscore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.config.Config;
import de.fenecon.femscore.config.JsonConfigFactory;
import de.fenecon.femscore.controller.ControllerWorker;
import de.fenecon.femscore.monitoring.MonitoringWorker;

/**
 * Main App
 *
 */
public class App {
	private final static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		Config config = JsonConfigFactory.readConfigFromJsonFile();
		log.info("Config: " + config);
		for (ControllerWorker controllerWorker : config.getControllers().values()) {
			controllerWorker.start();
		}
		for (MonitoringWorker monitoringWorker : config.getMonitorings().values()) {
			monitoringWorker.start();
		}
	}
}
