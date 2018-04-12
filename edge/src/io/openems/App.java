/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.restlet.engine.Engine;
import org.restlet.ext.slf4j.Slf4jLoggerFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.faljse.SDNotify.SDNotify;
import io.openems.core.Config;

public class App {
	private final static Logger log = LoggerFactory.getLogger(App.class);

	// public final static String OPENEMS_VERSION = "2018.5.0";
	public final static String OPENEMS_VERSION = "2018.6.0-SNAPSHOT";

	public static void main(String[] args) {
		log.info("OpenEMS version [" + OPENEMS_VERSION + "] started");
		// parse cli
		Option helpOption = Option.builder("h").longOpt("help").required(false).desc("shows this message").build();

		Option configFileOption = Option.builder("c").longOpt("configFile").numberOfArgs(1).required(false)
				.type(String.class).desc("path for the configFile").build();

		Options options = new Options();
		options.addOption(helpOption);
		options.addOption(configFileOption);

		CommandLineParser parser = new DefaultParser();
		CommandLine cmdLine;
		String configPath = null;
		try {
			cmdLine = parser.parse(options, args);

			if (cmdLine.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("openEMS", options);
				System.exit(0);
			} else {
				configPath = cmdLine.hasOption("configFile") ? (String) cmdLine.getParsedOptionValue("configFile")
						: null;
			}
		} catch (ParseException e1) {
			log.error("cli parsing failed!", e1);
		}
		// kick the watchdog: READY
		SDNotify.sendNotify();

		// configure Restlet logging
		Engine.getInstance().setLoggerFacade(new Slf4jLoggerFacade());

		// Get config
		try {
			Config config = Config.initialize(configPath);
			config.readConfigFile();
		} catch (Exception e) {
			App.shutdownWithError("OpenEMS Edge start failed", e);
		}

		log.info("OpenEMS Edge started");
		log.info("================================================================================");
	}

	public static void shutdownWithError(String message, Throwable t) {
		log.info("================================================================================");
		log.error(message + ": " + t.getMessage());
		t.printStackTrace();
		log.info("Exiting OpenEMS Edge");
		log.info("================================================================================");
		System.exit(1);
	}
}
