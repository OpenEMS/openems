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
