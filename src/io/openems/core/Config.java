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
package io.openems.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.Device;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.OpenemsException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.persistence.Persistence;
import io.openems.api.scheduler.Scheduler;
import io.openems.api.security.User;
import io.openems.core.utilities.AbstractWorker;
import io.openems.core.utilities.ConfigUtils;
import io.openems.core.utilities.InjectionUtils;
import io.openems.core.utilities.JsonUtils;

public class Config implements ChannelChangeListener {
	private final static Logger log = LoggerFactory.getLogger(Config.class);
	private static Config instance;

	public static synchronized Config getInstance() throws ConfigException {
		if (Config.instance == null) {
			Config.instance = new Config();
		}
		return Config.instance;
	}

	private final ThingRepository thingRepository;
	private final File file;

	public Config() throws ConfigException {
		thingRepository = ThingRepository.getInstance();
		this.file = getConfigFile();
	}

	public synchronized void readConfigFile()
			throws IOException, FileNotFoundException, ReflectionException, ConfigException, WriteChannelException {
		JsonObject jConfig = new JsonObject();
		log.info("Read configuration from " + file.getAbsolutePath());
		JsonParser parser = new JsonParser();
		JsonElement jsonElement = parser.parse(new FileReader(file));
		jConfig = jsonElement.getAsJsonObject();
		jConfig = addDefaultConfig(jConfig);
		// apply config
		parseJsonConfig(jConfig);
	}

	private JsonObject addDefaultConfig(JsonObject jConfig) {
		try {
			if (jConfig.has("things")) {
				JsonArray jThings = JsonUtils.getAsJsonArray(jConfig, "things");
				{
					/*
					 * System
					 */
					JsonObject jBridge = new JsonObject();
					JsonArray jDevices = new JsonArray();
					JsonObject jSystem = new JsonObject();
					jSystem.addProperty("class", "io.openems.impl.device.system.System");
					JsonObject jSystemNature = new JsonObject();
					jSystemNature.addProperty("id", "system0");
					jSystem.add("system", jSystemNature);
					jDevices.add(jSystem);
					jBridge.add("devices", jDevices);
					jThings.add(jBridge);
				}
			}
		} catch (ReflectionException e) {
			log.warn("Error applying default config: " + e.getMessage());
		}
		return jConfig;
	}

	public synchronized void writeConfigFile() throws OpenemsException {
		JsonObject jConfig = createJsonConfig();
		try (Writer writer = new FileWriter(file)) {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			gson.toJson(jConfig, writer);
			log.info("Wrote configuration to " + file.getAbsolutePath());
		} catch (IOException e) {
			throw new ConfigException("Unable to write to file [" + file.getAbsolutePath() + "]");
		}
	}

	public synchronized void parseJsonConfig(JsonObject jConfig)
			throws ReflectionException, ConfigException, WriteChannelException {
		/*
		 * read Users
		 */
		if (jConfig.has("users")) {
			JsonObject jUsers = JsonUtils.getAsJsonObject(jConfig, "users");
			for (Entry<String, JsonElement> jUsersElement : jUsers.entrySet()) {
				JsonObject jUser = JsonUtils.getAsJsonObject(jUsersElement.getValue());
				String username = jUsersElement.getKey();
				String passwordBase64 = JsonUtils.getAsString(jUser, "password");
				String saltBase64 = JsonUtils.getAsString(jUser, "salt");
				try {
					User.getUserByName(username).initialize(passwordBase64, saltBase64);
				} catch (OpenemsException e) {
					log.error("Error parsing config: " + e.getMessage());
				}
			}
		}
		User.initializeFinished(); // important! no more setting of users allowed!

		/*
		 * read each Bridge in "things" array
		 */
		JsonArray jThings = JsonUtils.getAsJsonArray(jConfig, "things");
		for (JsonElement jBridgeElement : jThings) {
			JsonObject jBridge = JsonUtils.getAsJsonObject(jBridgeElement);
			String bridgeClass = JsonUtils.getAsString(jBridge, "class");
			Bridge bridge = (Bridge) InjectionUtils.getThingInstance(bridgeClass);
			thingRepository.addThing(bridge);
			log.debug("Add Bridge[" + bridge.id() + "], Implementation[" + bridge.getClass().getSimpleName() + "]");
			ConfigUtils.injectConfigChannels(thingRepository.getConfigChannels(bridge), jBridge);
			/*
			 * read each Device in "things" array
			 */
			List<Device> devices = new ArrayList<>();
			JsonArray jDevices = JsonUtils.getAsJsonArray(jBridge, "devices");
			for (JsonElement jDeviceElement : jDevices) {
				JsonObject jDevice = JsonUtils.getAsJsonObject(jDeviceElement);
				String deviceClass = JsonUtils.getAsString(jDevice, "class");
				Device device = (Device) InjectionUtils.getThingInstance(deviceClass);
				thingRepository.addThing(device);
				log.debug("Add Device[" + device.id() + "], Implementation[" + device.getClass().getSimpleName() + "]");

				ConfigUtils.injectConfigChannels(thingRepository.getConfigChannels(device), jDevice);
				devices.add(device);
			}
			bridge.addDevices(devices);
		}

		/*
		 * read Scheduler
		 */
		if (jConfig.has("scheduler")) {
			JsonObject jScheduler = JsonUtils.getAsJsonObject(jConfig, "scheduler");
			String schedulerClass = JsonUtils.getAsString(jScheduler, "class");
			Scheduler scheduler = (Scheduler) InjectionUtils.getThingInstance(schedulerClass);
			thingRepository.addThing(scheduler);
			log.debug("Add Scheduler[" + scheduler.id() + "], Implementation[" + scheduler.getClass().getSimpleName()
					+ "]");
			ConfigUtils.injectConfigChannels(thingRepository.getConfigChannels(scheduler), jScheduler);
			/*
			 * read each Controller in "controllers" array
			 */
			JsonArray jControllers = JsonUtils.getAsJsonArray(jScheduler, "controllers");
			for (JsonElement jControllerElement : jControllers) {
				JsonObject jController = JsonUtils.getAsJsonObject(jControllerElement);
				String controllerClass = JsonUtils.getAsString(jController, "class");
				Controller controller = (Controller) InjectionUtils.getThingInstance(controllerClass);
				thingRepository.addThing(controller);
				log.debug("Add Controller[" + controller.id() + "], Implementation["
						+ controller.getClass().getSimpleName() + "]");
				ConfigUtils.injectConfigChannels(thingRepository.getConfigChannels(controller), jController);
				scheduler.addController(controller);
			}
		}

		/*
		 * read Persistence
		 */
		if (jConfig.has("persistence")) {
			JsonArray jPersistences = JsonUtils.getAsJsonArray(jConfig, "persistence");
			for (JsonElement jPersistenceElement : jPersistences) {
				JsonObject jPersistence = JsonUtils.getAsJsonObject(jPersistenceElement);
				String persistenceClass = JsonUtils.getAsString(jPersistence, "class");
				Persistence persistence = (Persistence) InjectionUtils.getThingInstance(persistenceClass);
				thingRepository.addThing(persistence);
				log.debug("Add Persistence[" + persistence.id() + "], Implementation["
						+ persistence.getClass().getSimpleName() + "]");
				ConfigUtils.injectConfigChannels(thingRepository.getConfigChannels(persistence), jPersistence);
			}
		}

		/*
		 * Configuration is finished -> start all worker threads
		 */
		thingRepository.getThings().forEach(thing -> {
			if (thing instanceof Thread) {
				((AbstractWorker) thing).start();
			}
		});

		/*
		 * Register myself as onChangeListener on all ConfigChannels
		 */
		for (ConfigChannel<?> channel : thingRepository.getConfigChannels()) {
			channel.changeListener(this);
		}
	}

	private synchronized JsonObject createJsonConfig() throws NotImplementedException {
		JsonObject jConfig = new JsonObject();
		/*
		 * Bridge
		 */
		JsonArray jBridges = new JsonArray();
		for (Bridge bridge : thingRepository.getBridges()) {
			JsonObject jBridge = (JsonObject) ConfigUtils.getAsJsonElement(bridge);
			/*
			 * Device
			 */
			JsonArray jDevices = new JsonArray();
			for (Device device : bridge.getDevices()) {
				JsonObject jDevice = (JsonObject) ConfigUtils.getAsJsonElement(device);
				jDevices.add(jDevice);
			}
			jBridge.add("devices", jDevices);
			jBridges.add(jBridge);
		}
		jConfig.add("things", jBridges);
		/*
		 * Scheduler
		 */
		JsonObject jScheduler = null;
		for (Scheduler scheduler : thingRepository.getSchedulers()) {
			jScheduler = (JsonObject) ConfigUtils.getAsJsonElement(scheduler);
			/*
			 * Controller
			 */
			JsonArray jControllers = new JsonArray();
			for (Controller controller : scheduler.getControllers()) {
				jControllers.add(ConfigUtils.getAsJsonElement(controller));
			}
			jScheduler.add("controllers", jControllers);
			break; // TODO only one Scheduler supported
		}
		jConfig.add("scheduler", jScheduler);
		/*
		 * Persistence
		 */
		JsonArray jPersistences = new JsonArray();
		for (Persistence persistence : thingRepository.getPersistences()) {
			JsonObject jPersistence = (JsonObject) ConfigUtils.getAsJsonElement(persistence);
			jPersistences.add(jPersistence);
		}
		jConfig.add("persistence", jPersistences);
		/*
		 * Users
		 */
		JsonObject jUsers = new JsonObject();
		for (User user : User.getUsers()) {
			JsonObject jUser = new JsonObject();
			jUser.addProperty("password", user.getPasswordBase64());
			jUser.addProperty("salt", user.getSaltBase64());
			jUsers.add(user.getName(), jUser);
		}
		jConfig.add("users", jUsers);
		return jConfig;
	}

	/**
	 * Receives update events for config channels and rewrites the json config
	 */
	@Override public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		try {
			writeConfigFile();
		} catch (OpenemsException e) {
			log.error("Config-Error.", e);
		}
	}

	/*
	 * Provides the File path of the config file ("/etc/openems.d/config.json") or a local file on a development machine
	 */
	private static File getConfigFile() throws ConfigException {
		File configFile = Paths.get("/etc", "openems.d", "config.json").toFile();
		if (!configFile.isFile()) {
			configFile = Paths.get("D:", "fems", "openems", "etc", "openems.d", "config.json").toFile();
		}
		if (!configFile.isFile()) {
			configFile = Paths.get("C:", "Users", "matthias.rossmann", "Dev", "git", "openems", "openems", "etc",
					"openems.d", "config.json").toFile();
		}
		if (!configFile.isFile()) {
			throw new ConfigException("No config file found!");
		}
		return configFile;
	}
}
