package io.openems.common.config;

import java.io.IOException;
import java.nio.file.Files;
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

@Component()
public class JsonPersistenceManager implements PersistenceManager {

	private final Logger log = LoggerFactory.getLogger(JsonPersistenceManager.class);

	private static final Path configFile = Paths.get(System.getProperty("configFile"));

	private final TreeMap<String, Config> configs = new TreeMap<>();

	@Activate
	void activate() {
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
		log.info("deactivate config");
		this.configs.clear();
	}

	@Override
	public void delete(String arg0) throws IOException {
		log.info("Delete " + arg0);
	}

	@Override
	public boolean exists(String arg0) {
		log.info("Exists " + arg0);
		return false;
	}

	@Override
	public Enumeration<Config> getDictionaries() throws IOException {
		return new ConfigEnumeration(this.configs.values().iterator());
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Dictionary load(String arg0) throws IOException {
		log.info("load " + arg0);
		return null;
	}

	@Override
	public void store(String pid, @SuppressWarnings("rawtypes") Dictionary values) throws IOException {
		log.debug("Store to Config. PID [" + pid + "]: " + values);

		synchronized (this.configs) {
			Config config = this.configs.get(pid);
			if (config == null) {
				config = new Config(pid);
			}
			Enumeration<?> keys = values.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				config.put(key, values.get(key));
			}
			this.configs.put(pid, config);
			
			this.saveConfigMapToFile();
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
}
