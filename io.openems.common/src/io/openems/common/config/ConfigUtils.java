package io.openems.common.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.Log;

public class ConfigUtils {

	private final static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	protected static synchronized JsonObject readConfigFromFile(Path path) throws Exception {
		if (!Files.exists(path)) {
			throw new IOException("Configuration file [" + path.toAbsolutePath() + "] not found!");
		}
		String config = new String(Files.readAllBytes(path), DEFAULT_CHARSET);
		return JsonUtils.parse(config).getAsJsonObject();
	}

	protected static synchronized void writeConfigToFile(Path path, TreeMap<String, Config> configs)
			throws IOException {
		// create JsonObject
		JsonObject j = new JsonObject();
		for (Entry<String, Config> entry : configs.entrySet()) {
			// ignore configs that should not be stored
			if (entry.getValue().isDoNotStore()) {
				continue;
			}

			JsonObject jSub = new JsonObject();
			// sort map by key to be able to write the json sorted
			TreeMap<String, Object> sortedSub = new TreeMap<>();
			for (Entry<String, Object> subEntry : entry.getValue().entrySet()) {
				sortedSub.put(subEntry.getKey(), subEntry.getValue());
			}

			for (Entry<String, Object> subEntry : sortedSub.entrySet()) {
				if (subEntry.getKey().equals("service.pid")) {
					// ignore. It's already the key of the JsonObject
					continue;
				}
				try {
					jSub.add(subEntry.getKey(), JsonUtils.getAsJsonElement(subEntry.getValue()));
				} catch (NotImplementedException e) {
					Log.warn("Unable to store [" + entry.getKey() + "/" + subEntry.getKey() + "] value ["
							+ subEntry.getValue() + "] in config: " + e.getMessage());
				}
			}
			j.add(entry.getKey(), jSub);
		}

		// write to file
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String config = gson.toJson(j);
		Files.write(path, config.getBytes(DEFAULT_CHARSET));
	}
}
