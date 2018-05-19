package io.openems.common.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.felix.cm.PersistenceManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

@Component(property = "ranking=100")
public class JsonPersistenceManager implements PersistenceManager {

	private final Logger log = LoggerFactory.getLogger(JsonPersistenceManager.class);

	private static final Path configFile = Paths.get(System.getProperty("configFile"));

	private final TreeMap<String, Config> configs = new TreeMap<>();

	@Activate
	void activate() {
		// Load default configuration
		loadDefaultConfig();
		
		// read Json from file
		JsonObject jConfig;
		try {
			jConfig = ConfigUtils.readConfigFromFile(configFile);
		} catch (Exception e) {
			log.error(e.getMessage());
			return;
		}

		// parse config + fill configMap
		parseJsonToConfigMap(jConfig);
	}

	@Deactivate
	void deactivate() {
		this.configs.clear();
	}

	@Override
	public void delete(String pid) throws IOException {
		synchronized (this.configs) {
			if (this.configs.remove(pid) != null) {
				this.saveConfigMapToFile();
			}
		}
	}

	@Override
	public boolean exists(String pid) {
		synchronized (this.configs) {
			return this.configs.containsKey(pid);
		}
	}

	@Override
	public Enumeration<Config> getDictionaries() throws IOException {
		return new ConfigEnumeration(this.configs.values().iterator());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Dictionary load(String pid) throws IOException {
		synchronized (this.configs) {
			return this.configs.get(pid);
		}
	}

	@Override
	public void store(String pid, @SuppressWarnings("rawtypes") Dictionary values) throws IOException {
		boolean configNeedsToBeAdded = false;
		boolean configChanged = false;
		synchronized (this.configs) {
			Config config = this.configs.get(pid);
			if (config == null) {
				config = new Config(pid);
				configNeedsToBeAdded = true;
				configChanged = true;
			}
			Enumeration<?> keys = values.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				Object newValue = values.get(key);
				Object existingValue = config.get(key);
				if (existingValue == null || (existingValue != null && !existingValue.equals(newValue))) {
					config.put(key, newValue);
					configChanged = true;
				}
			}
			if (configNeedsToBeAdded) {
				this.configs.put(pid, config);
			}
			if (configChanged) {
				this.saveConfigMapToFile();
			}
		}
	}

	private void parseJsonToConfigMap(JsonObject jConfig) {
		synchronized (this.configs) {
			for (Entry<String, JsonElement> configEntry : jConfig.entrySet()) {
				Config thisConfig = new Config(configEntry.getKey());
				if (configEntry.getValue().isJsonObject()) {
					JsonObject jThisConfig = configEntry.getValue().getAsJsonObject();
					for (Entry<String, JsonElement> thisConfigEntry : jThisConfig.entrySet()) {
						thisConfig.put(thisConfigEntry.getKey(), JsonUtils.getAsBestType(thisConfigEntry.getValue()));
					}
				}
				this.configs.put(thisConfig.getPid(), thisConfig);
			}
		}
	}

	private void saveConfigMapToFile() {
		synchronized (this.configs) {
			try {
				ConfigUtils.writeConfigToFile(configFile, configs);
			} catch (IOException e) {
				log.error("Unable to write config to file: " + e.getMessage());
			}
		}
	}
	
	private void loadDefaultConfig() {
		log.info("Load default config");
		synchronized (this.configs) {
			Config log4j = new Config("org.ops4j.pax.logging", true);
			log4j.put("log4j.rootLogger", "DEBUG, CONSOLE");
			log4j.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
			log4j.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
			log4j.put("log4j.appender.CONSOLE.layout.ConversionPattern",
                    "%d{ISO8601} [%-8.8t] %-5p [%-30.30c] - %m%n");
            // set minimum log levels for some verbose packages
			log4j.put("log4j.logger.org.eclipse.osgi", "WARN");
            log4j.put("log4j.logger.org.apache.felix.configadmin", "INFO");
            log4j.put("log4j.logger.sun.net.www.protocol.http.HttpURLConnection", "INFO");
            log4j.put("log4j.logger.io.openems", "INFO");
            this.configs.put(log4j.getPid(), log4j);
		}
		log.info("Finished Load default config");
	}
}
