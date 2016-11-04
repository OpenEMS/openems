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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class DemoJanitza extends Demo {

	@Override
	public JsonObject getConfig() {
		JsonObject config = new JsonObject();

		JsonArray things = new JsonArray();
		config.add("things", things);

		JsonObject bridge0 = new JsonObject();
		things.add(bridge0);
		bridge0.add("class", new JsonPrimitive("io.openems.impl.protocol.modbus.ModbusTcp"));
		bridge0.add("ip", new JsonPrimitive("192.168.178.150"));
		JsonArray devices0 = new JsonArray();
		bridge0.add("devices", devices0);

		JsonObject device0 = new JsonObject();
		devices0.add(device0);
		device0.add("class", new JsonPrimitive("io.openems.impl.device.janitza.JanitzaUMG96RME"));
		device0.add("modbusUnitId", new JsonPrimitive(1));

		JsonObject device1meter = new JsonObject();
		device0.add("meter", device1meter);
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

		return config;
	}

}
