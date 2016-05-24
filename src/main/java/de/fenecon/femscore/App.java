package de.fenecon.femscore;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.fenecon.femscore.config.Config;
import de.fenecon.femscore.config.JsonConfigFactory;
import de.fenecon.femscore.controller.ControllerWorker;

/**
 * Main App
 *
 */
public class App {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Config config = JsonConfigFactory.readConfigFromJsonFile();
		for (ControllerWorker controllerWorker : config.getControllerWorkers().values()) {
			controllerWorker.start();
		}
	}
}
