package io.openems.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class DemoFems7 extends Demo {
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
		bridge0.add("ip", new JsonPrimitive("127.0.0.1"));
		JsonArray devices = new JsonArray();
		bridge0.add("devices", devices);

		JsonObject device0 = new JsonObject();
		devices.add(device0);
		device0.add("class", new JsonPrimitive("io.openems.impl.device.commercial.FeneconCommercial"));
		device0.add("modbusUnitId", new JsonPrimitive(100));

		JsonObject device0ess = new JsonObject();
		device0.add("ess", device0ess);
		device0ess.add("thingId", new JsonPrimitive("ess0"));
		device0ess.add("minSoc", new JsonPrimitive(15));

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
		controller1.add("class", new JsonPrimitive("io.openems.impl.controller.balancing.Balancing"));

		return config;
	}
}
