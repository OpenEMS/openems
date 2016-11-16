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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.exception.ConfigException;
import io.openems.core.Config;
import io.openems.impl.api.rest.RestApi;
import io.openems.impl.api.websocket.WebsocketApi;
import io.vertx.core.Vertx;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	private final static int restApiPort = 8084;
	private final static int websocketPort = 8085;

	public static void main(String[] args) throws Exception {
		log.info("OpenEMS started");

		// Get config directory
		Path configPath = Paths.get("/etc", "openems.d");
		if (!configPath.toFile().exists()) {
			configPath = Paths.get("D:", "fems", "openems", "etc", "openems.d");
		}
		if (!configPath.toFile().exists()) {
			throw new ConfigException("No config directory found!");
		}

		// Load config
		Config config = new Config(configPath);
		config.parseConfigFiles();
		log.info("OpenEMS config loaded");

		// Wait for the important parts to start
		Thread.sleep(3000);

		// Start vertx
		Vertx vertx = Vertx.vertx();
		// deploy REST-Api
		vertx.deployVerticle(new RestApi(restApiPort), result -> {
			if (result.succeeded()) {
				log.info("REST-Api started on port [" + restApiPort + "]");
			} else {
				log.error("REST-Api failed on port [" + restApiPort + "]:", result.cause());
			}
		});

		// deploy Websocket-Api
		vertx.deployVerticle(new WebsocketApi(websocketPort), result -> {
			if (result.succeeded()) {
				log.info("Websocket started on port [" + websocketPort + "]");
			} else {
				log.error("Websocket failed on port [" + websocketPort + "]:", result.cause());
			}
		});
	}
}
