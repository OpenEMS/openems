package io.openems.common.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.util.Hashtable;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.Log;

public class ConfigUtils {

	private final static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

	public static synchronized void configureLogging(ConfigurationAdmin configAdmin) {
		Configuration configuration;
		try {
			configuration = configAdmin.getConfiguration("org.ops4j.pax.logging", null);
			final Hashtable<String, Object> log4jProps = new Hashtable<String, Object>();
			log4jProps.put("log4j.rootLogger", "DEBUG, CONSOLE");
			log4jProps.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
			log4jProps.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
			log4jProps.put("log4j.appender.CONSOLE.layout.ConversionPattern",
					"%d{ISO8601} [%-8.8t] %-5p [%-30.30c] - %m%n");
			// set minimum log levels for some verbose packages
			log4jProps.put("log4j.logger.org.eclipse.osgi", "WARN");
			log4jProps.put("log4j.logger.org.apache.felix.configadmin", "INFO");
			log4jProps.put("log4j.logger.sun.net.www.protocol.http.HttpURLConnection", "INFO");
			configuration.update(log4jProps);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	protected static synchronized JsonObject readConfigFromFile(Path path) throws Exception {
		try {
			String config = new String(Files.readAllBytes(path), DEFAULT_CHARSET);
			return JsonUtils.parse(config).getAsJsonObject();
		} catch (Exception e) {
			return new JsonObject();
		}
	}

	protected static synchronized void writeConfigToFile(Path path, TreeMap<String, Config> configs)
			throws IOException {
		// create JsonObject
		JsonObject j = new JsonObject();
		for (Entry<String, Config> entry : configs.entrySet()) {
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
