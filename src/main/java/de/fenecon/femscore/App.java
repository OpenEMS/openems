package de.fenecon.femscore;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.fenecon.femscore.controller.ControllerWorker;
import de.fenecon.femscore.controller.ControllerWorkerFactory;

/**
 * Main App
 *
 */
public class App {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		ControllerWorker controller = ControllerWorkerFactory.createControllerFromConfigFile();
		controller.start();
	}
}
