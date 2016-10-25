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
