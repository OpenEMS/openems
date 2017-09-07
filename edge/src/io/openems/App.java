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

import org.restlet.engine.Engine;
import org.restlet.ext.slf4j.Slf4jLoggerFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.faljse.SDNotify.SDNotify;
import io.openems.core.Config;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		log.info("OpenEMS started");

		// configure Restlet logging
		Engine.getInstance().setLoggerFacade(new Slf4jLoggerFacade());

		// Get config
		try {
			Config config = Config.getInstance();
			config.readConfigFile();
		} catch (Exception e) {
			log.error("OpenEMS Edge start failed: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		log.info("OpenEMS Edge started");
		log.info("================================================================================");

		// kick the watchdog: READY
		SDNotify.sendNotify();
	}
}
