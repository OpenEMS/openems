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

public class DemoFems7WithMeter extends Demo {
	public static void printConfig(JsonObject config) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(config));
	}

	@Override
	public JsonObject getConfig() {
		JsonObject config = new JsonObject();

		JsonArray things = new JsonArray();
		config.add("things", things);

		JsonObject bridge0 = new JsonObject();
		things.add(bridge0);
		bridge0.add("class", new JsonPrimitive("io.openems.impl.protocol.modbus.ModbusTcp"));
		bridge0.add("ip", new JsonPrimitive("10.4.0.15"));
		JsonArray devices0 = new JsonArray();
		bridge0.add("devices", devices0);

		JsonObject device0 = new JsonObject();
		devices0.add(device0);
		device0.add("class", new JsonPrimitive("io.openems.impl.device.commercial.FeneconCommercial"));
		device0.add("modbusUnitId", new JsonPrimitive(100));

		JsonObject device0ess = new JsonObject();
		device0.add("ess", device0ess);
		device0ess.add("thingId", new JsonPrimitive("ess0"));
		device0ess.add("minSoc", new JsonPrimitive(15));

		JsonObject bridge1 = new JsonObject();
		things.add(bridge1);
		bridge1.add("class", new JsonPrimitive("io.openems.impl.protocol.modbus.ModbusRtu"));
		bridge1.add("serialinterface", new JsonPrimitive("/dev/ttyUSB0"));
		bridge1.add("baudrate", new JsonPrimitive(38400));
		bridge1.add("databits", new JsonPrimitive(8));
		bridge1.add("parity", new JsonPrimitive("even"));
		bridge1.add("stopbits", new JsonPrimitive(1));
		JsonArray devices1 = new JsonArray();
		bridge1.add("devices", devices1);

		JsonObject device1 = new JsonObject();
		devices1.add(device1);
		device1.add("class", new JsonPrimitive("io.openems.impl.device.socomec.Socomec"));
		device1.add("modbusUnitId", new JsonPrimitive(5));

		JsonObject device1meter = new JsonObject();
		device1.add("meter", device1meter);
		device1meter.add("thingId", new JsonPrimitive("meter0"));

		JsonObject scheduler = new JsonObject();
		config.add("scheduler", scheduler);
		scheduler.add("class", new JsonPrimitive("io.openems.impl.scheduler.SimpleScheduler"));
		JsonArray controllers = new JsonArray();
		scheduler.add("controllers", controllers);

		JsonObject controller0 = new JsonObject();
		controllers.add(controller0);
		controller0.add("priority", new JsonPrimitive(150));
		controller0.add("class", new JsonPrimitive("io.openems.impl.controller.debuglog.DebugLogController"));

		JsonObject controller1 = new JsonObject();
		controllers.add(controller1);
		controller1.add("priority", new JsonPrimitive(100));
		controller1.add("class",
				new JsonPrimitive("io.openems.impl.controller.avoidtotaldischarge.AvoidTotalDischargeController"));

		JsonObject controller2 = new JsonObject();
		controllers.add(controller2);
		controller2.add("priority", new JsonPrimitive(50));
		controller2.add("class", new JsonPrimitive("io.openems.impl.controller.balancing.BalancingController"));

		JsonObject controller3 = new JsonObject();
		controllers.add(controller3);
		controller3.add("priority", new JsonPrimitive(0));
		controller3.add("class", new JsonPrimitive("io.openems.impl.controller.energysaver.EnergysavingController"));

		return config;
	}
}
