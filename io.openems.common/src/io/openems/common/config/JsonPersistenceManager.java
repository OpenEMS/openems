package io.openems.common.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.felix.cm.NotCachablePersistenceManager;
import org.apache.felix.cm.PersistenceManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

@Component(property = "ranking=100", immediate = true)
public class JsonPersistenceManager implements PersistenceManager, NotCachablePersistenceManager {

	private final Logger log = LoggerFactory.getLogger(JsonPersistenceManager.class);

	private final static Path CONFIG_FILE = Paths.get(System.getProperty("configFile"));

	private final TreeMap<String, Config> configs = new TreeMap<>();

	@Activate
	void activate() {
		// Load default configuration
		loadDefaultConfig();

		// read Json from file
		JsonArray jConfig;
		try {
			jConfig = ConfigUtils.readConfigFromFile(CONFIG_FILE);
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
			// Throw error if this "id" is already existing
			String newId = (String) values.get("id");
			if (newId != null) {
				for (Config existingConfig : this.configs.values()) {
					String existingId = existingConfig.getIdOpt().orElse(null);
					if (existingId != null && newId.equals(existingId)) {
						throw new IOException("Unable to store ID [" + newId
								+ "]. A configuration with the same ID is already existing.");
					}
				}
			}

			Config config = this.configs.get(pid);
			if (config == null) {
				config = new Config(pid, newId);
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

	private void parseJsonToConfigMap(JsonArray jConfigArray) {
		int nextPid = 0;
		synchronized (this.configs) {
			for (JsonElement jConfigElement : jConfigArray) {
				try {
					JsonObject jConfig = JsonUtils.getAsJsonObject(jConfigElement);
					String pid = JsonUtils.getAsOptionalString(jConfig, "service.pid").orElse("pid" + nextPid++);
					String id = JsonUtils.getAsOptionalString(jConfig, "id").orElse("");
					Config thisConfig = new Config(pid, id);
					for (Entry<String, JsonElement> thisConfigEntry : jConfig.entrySet()) {
						String key = thisConfigEntry.getKey();
						JsonElement jValue = thisConfigEntry.getValue();
						try {
							thisConfig.put(key, JsonUtils.getAsBestType(jValue));
						} catch (OpenemsException e) {
							log.error("Config failed [" + key + ":" + jValue + "]: " + e.getMessage());
						}
						/*
						 * Find configuration keys in the form "{name}.id" or "{name}.ids". If found, a
						 * new configuration property for "{name}.target" is created. This automates the
						 * mapping of "@Reference"s to OpenemsComponents. Example: - items.ids = ['id0',
						 * 'id1'] creates target filter '(|(id=id0)(id=id1))' - item.id = 'id0' creates
						 * target filter '(id=id0)'
						 */
						if (key.endsWith(".ids") || key.endsWith(".id")) {
							// create target filter
							String target;
							if (jValue.isJsonArray()) {
								StringBuilder targetBuilder = new StringBuilder("(|");
								for (JsonElement j : jValue.getAsJsonArray()) {
									targetBuilder.append("(id=" + j.getAsString() + ")");
								}
								targetBuilder.append(")");
								target = targetBuilder.toString();
							} else {
								target = "(id=" + jValue.getAsString() + ")";
							}
							// remove suffix
							if (key.endsWith(".ids")) {
								key = key.substring(0, key.length() - 4);
							} else {
								key = key.substring(0, key.length() - 3);
							}
							// add config
							thisConfig.put(key + ".target", target);
						}
					}
					this.configs.put(pid, thisConfig);
				} catch (OpenemsException e) {
					log.warn("Unable to parse config [" + jConfigElement + "]: " + e.getMessage());
				}
			}
		}
	}

	private void saveConfigMapToFile() {
		synchronized (this.configs) {
			try {
				ConfigUtils.writeConfigToFile(JsonPersistenceManager.CONFIG_FILE, this.configs.values());
			} catch (IOException e) {
				log.error("Unable to write config to file: " + e.getMessage());
			}
		}
	}

	private void loadDefaultConfig() {
		synchronized (this.configs) {
			Config log4j = new Config("org.ops4j.pax.logging", true);
			log4j.put("log4j.rootLogger", "DEBUG, CONSOLE");
			log4j.put("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
			log4j.put("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
			log4j.put("log4j.appender.CONSOLE.layout.ConversionPattern", "%d{ISO8601} [%-8.8t] %-5p [%-30.30c] - %m%n");
			// set minimum log levels for some verbose packages
			log4j.put("log4j.logger.org.eclipse.osgi", "WARN");
			log4j.put("log4j.logger.org.apache.felix.configadmin", "INFO");
			log4j.put("log4j.logger.sun.net.www.protocol.http.HttpURLConnection", "INFO");
			this.configs.put(log4j.getPid(), log4j);
		}
	}
}
