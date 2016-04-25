package de.fenecon.femscore;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.fenecon.femscore.controller.Controller;
import de.fenecon.femscore.controller.ControllerFactory;

/**
 * Main App
 *
 */
public class App {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		Controller controller = ControllerFactory.createControllerFromConfigFile();
		controller.start();
	}
}
