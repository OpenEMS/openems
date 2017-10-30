/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import io.openems.api.doc.ThingDoc;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.OpenemsException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.persistence.Persistence;
import io.openems.api.scheduler.Scheduler;
import io.openems.api.security.User;
import io.openems.api.thing.Thing;
import io.openems.common.session.Role;
import io.openems.core.utilities.ConfigUtils;
import io.openems.core.utilities.InjectionUtils;
import io.openems.core.utilities.JsonUtils;

public class Config implements ChannelChangeListener {

	private final static Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
	private final static String CONFIG_PATH = "etc/openems.d";
	private final static String CONFIG_FILE_NAME = "config.json";
	private final static String CONFIG_BACKUP_FILE_NAME = "config.backup.json";

	private final static Logger log = LoggerFactory.getLogger(Config.class);
	private static Config instance;
	private List<BridgeInitializedEventListener> bridgeInitEventListeners = new ArrayList<>();
	private List<SchedulerInitializedEventListener> schedulerInitEventListeners = new ArrayList<>();

	public static synchronized Config getInstance() throws ConfigException {
		if (Config.instance == null) {
			throw new ConfigException("Config is not initialized please call initialize() firs!");
		}
		return Config.instance;
	}

	public static synchronized Config initialize(String path) throws ConfigException {
		if (Config.instance == null) {
			Config.instance = new Config(path);
			return Config.instance;
		} else {
			throw new ConfigException("Config is already initialized!");
		}
	}

	private final ThingRepository thingRepository;
	private final Path configFile;
	private final Path configBackupFile;
	private final ExecutorService writeConfigExecutor;

	public Config(String configPath) throws ConfigException {
		thingRepository = ThingRepository.getInstance();
		if (configPath != null) {
			configFile = Paths.get(configPath);
		} else {
			this.configFile = getConfigFile();
		}
		this.configBackupFile = getConfigBackupFile();
		this.writeConfigExecutor = Executors.newSingleThreadExecutor();
	}

	public void addBridgeInitializedEventListener(BridgeInitializedEventListener listener) {
		this.bridgeInitEventListeners.add(listener);
	}

	public void removeBridgeInitializedEventListener(BridgeInitializedEventListener listener) {
		this.bridgeInitEventListeners.remove(listener);
	}

	public void addSchedulerInitializedEventListener(SchedulerInitializedEventListener listener) {
		this.schedulerInitEventListeners.add(listener);
	}

	public void removeSchedulerInitializedEventListener(SchedulerInitializedEventListener listener) {
		this.schedulerInitEventListeners.remove(listener);
	}

	public synchronized void readConfigFile() throws Exception {
		// Read configuration from default config file
		try {
			readConfigFromFile(configFile);
			log.info("Read configuration from file [" + configFile.toString() + "]");
			return;
		} catch (Exception e) {
			log.warn("Failed to read configuration from file [" + configFile.toString() + "] ",e);
		}
		// Read configuration from backup config file
		try {
			readConfigFromFile(configBackupFile);
			log.info("Read configuration from backup file [" + configBackupFile.toString() + "]");
		} catch (Exception e) {
			log.warn("Failed to read configuration backup file [" + configFile.toString() + "]", e);
			throw e;
		}
	}

	private synchronized void readConfigFromFile(Path path) throws Exception {
		JsonObject jConfig = new JsonObject();
		JsonParser parser = new JsonParser();
		String config = new String(Files.readAllBytes(path), DEFAULT_CHARSET);
		JsonElement jsonElement = parser.parse(config);
		jConfig = jsonElement.getAsJsonObject();
		jConfig = addDefaultConfig(jConfig);
		// apply config
		parseJsonConfig(jConfig);
	}

	private JsonObject addDefaultConfig(JsonObject jConfig) {
		try {
			/*
			 * Things
			 */
			JsonArray jThings;
			if (!jConfig.has("things") || !jConfig.get("things").isJsonArray()) {
				jThings = new JsonArray();
			} else {
				jThings = JsonUtils.getAsJsonArray(jConfig, "things");
			}
			{
				/*
				 * Add SystemBridge
				 */
				if (!JsonUtils.hasElement(jConfig, "things", "class", "io.openems.impl.protocol.system.SystemBridge")) {
					JsonObject jBridge = new JsonObject();
					{
						jBridge.addProperty("class", "io.openems.impl.protocol.system.SystemBridge");
						JsonArray jDevices = new JsonArray();
						{
							JsonObject jSystem = new JsonObject();
							jSystem.addProperty("class", "io.openems.impl.device.system.System");
							{
								JsonObject jSystemNature = new JsonObject();
								{
									jSystemNature.addProperty("id", "system0");
								}
								jSystem.add("system", jSystemNature);
							}
							jDevices.add(jSystem);
						}
						jBridge.add("devices", jDevices);
					}
					jThings.add(jBridge);
				}
			}
			jConfig.add("things", jThings);
			/*
			 * Scheduler
			 */
			JsonObject jScheduler;
			if (!jConfig.has("scheduler") || !jConfig.get("scheduler").isJsonObject()) {
				jScheduler = new JsonObject();
				jScheduler.addProperty("class", "io.openems.impl.scheduler.SimpleScheduler");
			} else {
				jScheduler = JsonUtils.getAsJsonObject(jConfig, "scheduler");
			}
			{
				/*
				 * Controller
				 */
				JsonArray jControllers;
				if (!jScheduler.has("controllers") || !jScheduler.get("controllers").isJsonArray()) {
					jControllers = new JsonArray();
				} else {
					jControllers = JsonUtils.getAsJsonArray(jScheduler, "controllers");
				}
				{
					/*
					 * WebsocketApiController
					 */
					if (!JsonUtils.hasElement(jControllers, "class",
							"io.openems.impl.controller.api.websocket.WebsocketApiController")) {
						JsonObject jWebsocketApiController = new JsonObject();
						jWebsocketApiController.addProperty("class",
								"io.openems.impl.controller.api.websocket.WebsocketApiController");
						jWebsocketApiController.addProperty("priority", Integer.MIN_VALUE);
						jControllers.add(jWebsocketApiController);
					}
					/*
					 * RestApiController
					 */
					if (!JsonUtils.hasElement(jControllers, "class",
							"io.openems.impl.controller.api.rest.RestApiController")) {
						JsonObject jRestApiController = new JsonObject();
						jRestApiController.addProperty("class",
								"io.openems.impl.controller.api.rest.RestApiController");
						jRestApiController.addProperty("priority", Integer.MIN_VALUE);
						jControllers.add(jRestApiController);
					}
				}
				jScheduler.add("controllers", jControllers);
			}
			jConfig.add("scheduler", jScheduler);
		} catch (ReflectionException e) {
			log.warn("Error applying default config: " + e.getMessage());
		}
		return jConfig;
	}

	/**
	 * Writes the config file. Holds a backup config file and restores it on error. Method is executed asynchronously.
	 *
	 * @throws NotImplementedException
	 */
	public void writeConfigFile() throws NotImplementedException {
		// TODO send config to all attached websockets
		// get config as json
		JsonObject jConfig = this.getJson(ConfigFormat.FILE, Role.ADMIN, "en");

		Runnable writeConfigRunnable = new Runnable() {
			@Override
			public void run() {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				String config = gson.toJson(jConfig);
				try {
					/*
					 * create backup of config file
					 */
					Files.copy(configFile, configBackupFile, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					ConfigException ex = new ConfigException(
							"Unable to create backup file [" + configBackupFile.toString() + "]");
					log.error(ex.getMessage(), ex);
				}

				try {
					/*
					 * write config file
					 */
					Files.write(configFile, config.getBytes(DEFAULT_CHARSET));
				} catch (IOException e) {
					ConfigException ex = new ConfigException(
							"Unable to write config file [" + configFile.toString() + "]", e);
					log.error(ex.getMessage(), ex);

					try {
						/*
						 * On error: recover backup file
						 */
						Files.copy(configBackupFile, configFile, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e2) {
						ConfigException ex2 = new ConfigException(
								"Unable to recover backup file [" + configBackupFile.toString() + "]");
						log.error(ex2.getMessage(), ex2);
					}
				}
			}
		};
		this.writeConfigExecutor.execute(writeConfigRunnable);
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
			log.info("Add Bridge[" + bridge.id() + "], Implementation[" + bridge.getClass().getSimpleName() + "]");
			ConfigUtils.injectConfigChannels(thingRepository.getConfigChannels(bridge), jBridge);
			/*
			 * read each Device in "things" array
			 */
			List<Device> devices = new ArrayList<>();
			JsonArray jDevices = JsonUtils.getAsJsonArray(jBridge, "devices");
			for (JsonElement jDeviceElement : jDevices) {
				JsonObject jDevice = JsonUtils.getAsJsonObject(jDeviceElement);
				Device device = thingRepository.createDevice(jDevice, bridge);
				devices.add(device);
				bridge.addDevice(device);
			}
		}
		/*
		 * Init bridge
		 */
		for (Bridge b : thingRepository.getBridges()) {
			for (Device d : b.getDevices()) {
				d.init();
			}
			b.init();
		}
		for(BridgeInitializedEventListener listener : bridgeInitEventListeners) {
			listener.onBridgeInitialized();
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
				Controller controller = thingRepository.createController(jController);
				scheduler.addController(controller);
				controller.init();
			}
			scheduler.init();
		}
		for(SchedulerInitializedEventListener listener: schedulerInitEventListeners) {
			listener.onSchedulerInitialized();
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
				log.info("Add Persistence[" + persistence.id() + "], Implementation["
						+ persistence.getClass().getSimpleName() + "]");
				ConfigUtils.injectConfigChannels(thingRepository.getConfigChannels(persistence), jPersistence);
				persistence.init();
			}
		}

		/*
		 * Configuration is finished -> apply again channel annotation to all of them because many channels are only
		 * defined during init()
		 */
		thingRepository.getThings().forEach(thing -> {
			thingRepository.applyChannelAnnotation(thing);
		});

		/*
		 * Start all worker threads
		 */
		thingRepository.getThings().forEach(thing -> {
			// TODO use executor
			if (thing instanceof Thread) {
				((Thread) thing).start();
			}
		});

		/*
		 * Register myself as onChangeListener on all ConfigChannels
		 */
		for (ConfigChannel<?> channel : thingRepository.getConfigChannels()) {
			channel.addChangeListener(this);
		}

		/*
		 * After 10 seconds: build the ClassRepository cache to speed up future calls
		 * (this speeds up the first opening of the UI, as the cache does not need to be built)
		 */
		Executors.newScheduledThreadPool(1).schedule(() -> {
			try {
				ClassRepository.getInstance().getAvailableThings();
			} catch (ReflectionException e) { /* ignore */}
		}, 10, TimeUnit.SECONDS);
	}

	public JsonArray getBridgesJson(ConfigFormat format, Role role) throws NotImplementedException {
		JsonArray jBridges = new JsonArray();
		for (Bridge bridge : thingRepository.getBridges()) {
			JsonObject jBridge = (JsonObject) ConfigUtils.getAsJsonElement(bridge, format, role);
			/*
			 * Device
			 */
			JsonArray jDevices = new JsonArray();
			for (Device device : bridge.getDevices()) {
				JsonObject jDevice = (JsonObject) ConfigUtils.getAsJsonElement(device, format, role);
				jDevices.add(jDevice);
			}
			jBridge.add("devices", jDevices);
			jBridges.add(jBridge);
		}
		return jBridges;
	}

	public JsonObject getSchedulerJson(ConfigFormat format, Role role) throws NotImplementedException {
		JsonObject jScheduler = null;
		for (Scheduler scheduler : thingRepository.getSchedulers()) {
			jScheduler = (JsonObject) ConfigUtils.getAsJsonElement(scheduler, format, role);
			/*
			 * Controller
			 */
			JsonArray jControllers = new JsonArray();
			for (Controller controller : scheduler.getControllers()) {
				jControllers.add(ConfigUtils.getAsJsonElement(controller, format, role));
			}
			jScheduler.add("controllers", jControllers);
			break; // TODO only one Scheduler supported
		}
		return jScheduler;
	}

	public JsonArray getPersistenceJson(ConfigFormat format, Role role) throws NotImplementedException {
		JsonArray jPersistences = new JsonArray();
		for (Persistence persistence : thingRepository.getPersistences()) {
			JsonObject jPersistence = (JsonObject) ConfigUtils.getAsJsonElement(persistence, format, role);
			jPersistences.add(jPersistence);
		}
		return jPersistences;
	}

	private JsonObject getUsersJson() {
		JsonObject jUsers = new JsonObject();
		for (User user : User.getUsers()) {
			JsonObject jUser = new JsonObject();
			jUser.addProperty("password", user.getPasswordBase64());
			jUser.addProperty("salt", user.getSaltBase64());
			jUsers.add(user.getName(), jUser);
		}
		return jUsers;
	}

	/**
	 * Gets the Config as Json in the given format
	 *
	 * @param format
	 * @return
	 * @throws NotImplementedException
	 */
	// TODO make use of language tag Enum
	public synchronized JsonObject getJson(ConfigFormat format, Role role, String language)
			throws NotImplementedException {
		JsonObject jConfig = new JsonObject();
		if (format == ConfigFormat.FILE) {
			/*
			 * Prepare Json in format for config.json file
			 */
			// Bridge
			jConfig.add("things", getBridgesJson(format, role));
			// Scheduler
			jConfig.add("scheduler", getSchedulerJson(format, role));
			// Persistence
			jConfig.add("persistence", getPersistenceJson(format, role));
			// Users
			jConfig.add("users", getUsersJson());

		} else {
			/*
			 * Prepare Json in format for OpenEMS UI
			 */
			// things...
			JsonObject jThings = new JsonObject();
			Set<Thing> things = ThingRepository.getInstance().getThings();
			for (Thing thing : things) {
				JsonObject jThing = (JsonObject) ConfigUtils.getAsJsonElement(thing, format, role);
				jThings.add(thing.id(), jThing);
			}
			jConfig.add("things", jThings);
			// meta...
			JsonObject jMeta = new JsonObject();
			try {
				Iterable<ThingDoc> availableThings = ClassRepository.getInstance().getAvailableThings();
				for (ThingDoc availableThing : availableThings) {
					jMeta.add(availableThing.getClazz().getName(), availableThing.getAsJsonObject());
				}
			} catch (ReflectionException e) {
				log.error(e.getMessage());
				e.printStackTrace();
			}
			jConfig.add("meta", jMeta);
		}
		return jConfig;
	}

	/**
	 * Receives update events for config channels and rewrites the json config
	 */
	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		// TODO: trigger ConfigUpdated event
		try {
			writeConfigFile();
		} catch (OpenemsException e) {
			log.error("Config-Error.", e);
		}
	}

	/**
	 * Provides the File path of the config file ("/etc/openems.d/config.json") or a local file on a development machine
	 */
	private Path getConfigFile() {
		String relativePath = CONFIG_PATH + "/" + CONFIG_FILE_NAME;
		Path configFile = Paths.get(relativePath);
		if (Files.isReadable(configFile)) {
			// we are on development system
			return configFile;
		}
		return Paths.get("/" + relativePath);
	}

	/**
	 * Provides the File path of the config backup file ("/etc/openems.d/config.backup.json") or a local file on a
	 * development machine
	 */
	private Path getConfigBackupFile() {
		Path configFile = getConfigFile();
		Path backupFile = configFile.getParent().resolve(CONFIG_BACKUP_FILE_NAME);
		return backupFile;
	}
}