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
package io.openems.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class DemoPro {
	public static void printConfig(JsonObject config) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(config));
	}

	public JsonObject getConfig() {
		JsonObject config = new JsonObject();

		JsonArray things = new JsonArray();
		config.add("things", things);

		JsonObject bridge0 = new JsonObject();
		things.add(bridge0);
		bridge0.add("class", new JsonPrimitive("io.openems.impl.bridge.modbus.ModbusRtu"));
		bridge0.add("serialinterface", new JsonPrimitive("/dev/ttyUSB0"));
		bridge0.add("baudrate", new JsonPrimitive(38400));
		bridge0.add("databits", new JsonPrimitive(8));
		bridge0.add("parity", new JsonPrimitive("even"));
		bridge0.add("stopbits", new JsonPrimitive(1));
		JsonArray devices = new JsonArray();
		bridge0.add("devices", devices);

		JsonObject device0 = new JsonObject();
		devices.add(device0);
		device0.add("class", new JsonPrimitive("io.openems.impl.device.pro.FeneconPro"));
		device0.add("modbusUnitId", new JsonPrimitive(100));

		JsonObject device0ess = new JsonObject();
		device0.add("ess", device0ess);
		device0ess.add("thingId", new JsonPrimitive("ess0"));
		device0ess.add("minSoc", new JsonPrimitive(15));

		JsonObject device0meter = new JsonObject();
		device0.add("meter", device0meter);
		device0meter.add("thingId", new JsonPrimitive("meter0"));

		JsonObject scheduler = new JsonObject();
		config.add("scheduler", scheduler);
		scheduler.add("class", new JsonPrimitive("io.openems.impl.scheduler.SimpleScheduler"));
		JsonArray controllers = new JsonArray();
		scheduler.add("controllers", controllers);

		JsonObject controller0 = new JsonObject();
		controllers.add(controller0);
		controller0.add("priority", new JsonPrimitive(100));
		controller0.add("class",
				new JsonPrimitive("io.openems.impl.controller.avoidtotaldischarge.AvoidTotalDischargeController"));

		JsonObject controller1 = new JsonObject();
		controllers.add(controller1);
		controller1.add("priority", new JsonPrimitive(50));
		controller1.add("class", new JsonPrimitive("io.openems.impl.controller.balancing.BalancingController"));
		controller1.add("chargeFromAc", new JsonPrimitive(true));
		controller1.add("gridMeter", new JsonPrimitive("meter0"));

		return config;
	}
}
