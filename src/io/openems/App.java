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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.core.Config;
import io.openems.demo.Demo;
import io.openems.demo.DemoSimulator;
import io.openems.impl.api.rest.RestApi;
import io.openems.impl.api.websocket.WebSocketClient;
import io.openems.impl.api.websocket.WebsocketApi;
import io.vertx.core.Vertx;

public class App {
	private static Logger log = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) throws Exception {
		log.info("OpenEMS started");

		// Demo demo = new DemoFems7WithMeter();
		// Demo demo = new DemoJanitza();
		// Demo demo = new DemoFems7();
		Demo demo = new DemoSimulator();
		JsonObject jConfig = demo.getConfig();
		//
		// File file = new File("config");
		// log.info("Read configuration from " + file.getAbsolutePath());
		// JsonParser parser = new JsonParser();
		// JsonElement jsonElement = parser.parse(new FileReader(file));
		// JsonObject jConfig = jsonElement.getAsJsonObject();

		Config config = new Config();
		config.readConfig(jConfig);

		// log.info("OpenEMS config loaded");

		// databus.printAll();

		// Start vertx
		Vertx vertx = Vertx.vertx();
		// Deploy REST-Api verticle
		vertx.deployVerticle(new RestApi());

		// Thread.sleep(3000);

		// Databus databus = Databus.getInstance();
		// log.info("ess0/soc: " + databus.getValue("ess0", "Soc"));

		vertx.deployVerticle(new WebsocketApi());
		vertx.deployVerticle(new WebSocketClient());
	}
}
