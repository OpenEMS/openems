package io.openems.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public abstract class Demo {
	public static void printConfig(JsonObject config) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		System.out.println(gson.toJson(config));
	}

	public abstract JsonObject getConfig();
}
