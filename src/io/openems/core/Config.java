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
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.Device;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.persistence.Persistence;
import io.openems.api.scheduler.Scheduler;
import io.openems.api.thing.Thing;
import io.openems.core.utilities.InjectionUtils;
import io.openems.core.utilities.JsonUtils;

public class Config {
	private final static Logger log = LoggerFactory.getLogger(Config.class);

	private final ThingRepository thingRepository;

	private final Path path;

	public Config(Path path) {
		thingRepository = ThingRepository.getInstance();
		this.path = path;
	}

	public void parseConfigFiles()
			throws IOException, FileNotFoundException, ReflectionException, ConfigException, WriteChannelException {
		JsonObject jConfig = new JsonObject();
		// read files in path directory
		for (final File file : path.toFile().listFiles()) {
			log.info("Read configuration from " + file.getAbsolutePath());
			JsonParser parser = new JsonParser();
			JsonElement jsonElement = parser.parse(new FileReader(file));
			jConfig = jsonElement.getAsJsonObject();
			// TODO: read all files in folder and merge them
			continue;
		}
		// apply config
		readConfig(jConfig);
	}

	public void readConfig(JsonObject jConfig) throws ReflectionException, ConfigException, WriteChannelException {
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
			injectConfigChannels(thingRepository.getConfigChannels(bridge), jBridge);
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

				injectConfigChannels(thingRepository.getConfigChannels(device), jDevice);
				devices.add(device);
			}
			bridge.addDevices(devices);
		}

		/*
		 * read Scheduler
		 */
		JsonObject jScheduler = JsonUtils.getAsJsonObject(jConfig, "scheduler");
		String schedulerClass = JsonUtils.getAsString(jScheduler, "class");
		Scheduler scheduler = (Scheduler) InjectionUtils.getThingInstance(schedulerClass);
		thingRepository.addThing(scheduler);
		log.debug(
				"Add Scheduler[" + scheduler.id() + "], Implementation[" + scheduler.getClass().getSimpleName() + "]");
		injectConfigChannels(thingRepository.getConfigChannels(scheduler), jScheduler);
		/*
		 * read each Controller in "controllers" array
		 */
		JsonArray jControllers = JsonUtils.getAsJsonArray(jScheduler, "controllers");
		for (JsonElement jControllerElement : jControllers) {
			JsonObject jController = JsonUtils.getAsJsonObject(jControllerElement);
			String controllerClass = JsonUtils.getAsString(jController, "class");
			Controller controller = (Controller) InjectionUtils.getThingInstance(controllerClass);
			thingRepository.addThing(controller);
			log.debug("Add Controller[" + controller.id() + "], Implementation[" + controller.getClass().getSimpleName()
					+ "]");
			injectConfigChannels(thingRepository.getConfigChannels(controller), jController);
			scheduler.addController(controller);
		}

		/*
		 * read Persistence
		 */
		JsonArray jPersistences = JsonUtils.getAsJsonArray(jConfig, "persistence");
		for (JsonElement jPersistenceElement : jPersistences) {
			JsonObject jPersistence = JsonUtils.getAsJsonObject(jPersistenceElement);
			String persistenceClass = JsonUtils.getAsString(jPersistence, "class");
			Persistence persistence = (Persistence) InjectionUtils.getThingInstance(persistenceClass);
			thingRepository.addThing(persistence);
			log.debug("Add Persistence[" + persistence.id() + "], Implementation["
					+ persistence.getClass().getSimpleName() + "]");
			injectConfigChannels(thingRepository.getConfigChannels(persistence), jPersistence);
		}
	}

	private Thing getThingFromConfig(Class<?> type, JsonElement j) throws ReflectionException {
		String thingId = JsonUtils.getAsString(j, "id");
		Optional<Thing> existingThing = thingRepository.getThingById(thingId);
		Thing thing;
		if (existingThing.isPresent()) {
			// reuse existing Thing
			thing = existingThing.get();
		} else {
			// Thing is not existing. Create a new instance
			thing = InjectionUtils.getThingInstance(type, thingId);
			thingRepository.addThing(thing);
			log.debug("Add Thing[" + thing.id() + "], Implementation[" + thing.getClass().getSimpleName() + "]");
		}
		// Recursive call to inject config parameters for the newly created Thing
		injectConfigChannels(thingRepository.getConfigChannels(thing), j.getAsJsonObject());
		return thing;
	}

	private Object getConfigObject(ConfigChannel<?> channel, JsonElement j) throws ReflectionException {
		Class<?> type = channel.type();
		if (Integer.class.isAssignableFrom(type)) {
			/*
			 * Asking for an Integer
			 */
			return j.getAsInt();

		} else if (String.class.isAssignableFrom(type)) {
			/*
			 * Asking for a String
			 */
			return j.getAsString();

		} else if (Thing.class.isAssignableFrom(type)) {
			/*
			 * Asking for a Thing
			 */
			return getThingFromConfig(type, j);

		} else if (ThingMap.class.isAssignableFrom(type)) {
			/*
			 * Asking for a ThingMap
			 */
			return getThingMapsFromConfig(channel, j);

		} else if (Inet4Address.class.isAssignableFrom(type)) {
			/*
			 * Asking for an IPv4
			 */
			try {
				return Inet4Address.getByName(j.getAsString());
			} catch (UnknownHostException e) {
				throw new ReflectionException("Unable to convert [" + j + "] to IPv4 address");
			}
		}
		throw new ReflectionException("Unable to match config [" + j + "] to class type [" + type + "]");
	}

	/**
	 * Fill all Config-Channels from a JsonObject configuration
	 *
	 * @param channels
	 * @param jConfig
	 * @throws ConfigException
	 */
	private void injectConfigChannels(Set<ConfigChannel<?>> channels, JsonObject jConfig) throws ReflectionException {
		for (ConfigChannel<?> channel : channels) {
			if ((!jConfig.has(channel.id()) && channel.valueOptional().isPresent()) || channel.isOptional()) {
				// Element for this Channel is not existing existing in the configuration, but a default value was set
				continue;
			}
			JsonElement jChannel = JsonUtils.getSubElement(jConfig, channel.id());
			Object parameter = getConfigObject(channel, jChannel);
			channel.updateValue(parameter, true);
		}

	}

	private Object getThingMapsFromConfig(ConfigChannel<?> channel, JsonElement j) throws ReflectionException {
		/*
		 * Get "Field" in Channels parent class
		 */
		Field field;
		try {
			field = channel.parent().getClass().getField(channel.id());
		} catch (NoSuchFieldException | SecurityException e) {
			throw new ReflectionException("Field for ConfigChannel [" + channel.address() + "] is not named ["
					+ channel.id() + "] in [" + channel.getClass().getSimpleName() + "]");
		}

		/*
		 * Get expected Object Type (List, Set, simple Object)
		 */
		Type expectedObjectType = ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
		if (expectedObjectType instanceof ParameterizedType) {
			expectedObjectType = ((ParameterizedType) expectedObjectType).getRawType();
		}
		Class<?> expectedObjectClass = (Class<?>) expectedObjectType;

		/*
		 * Get the ThingMap class
		 */
		Class<?> thingMapClass = channel.type();

		/*
		 * Get the referenced Thing class
		 */
		IsThingMap isThingMapAnnotation = thingMapClass.getAnnotation(IsThingMap.class);
		Class<? extends Thing> thingClass = isThingMapAnnotation.type();

		/*
		 * Prepare filter for matching Things
		 * - Empty filter: accept everything
		 * - Otherwise: accept only exact string matches on the thing id
		 */
		Set<String> filter = new HashSet<>();
		if (j.isJsonPrimitive()) {
			String id = j.getAsJsonPrimitive().getAsString();
			if (!id.equals("*")) {
				filter.add(id);
			}
		} else if (j.isJsonArray()) {
			j.getAsJsonArray().forEach(id -> filter.add(id.getAsString()));
		}

		/*
		 * Create ThingMap instance(s) for each matching Thing
		 */
		Set<Thing> matchingThings = thingRepository.getThingsAssignableByClass(thingClass);
		Set<ThingMap> thingMaps = new HashSet<>();
		for (Thing thing : matchingThings) {
			if (filter.isEmpty() || filter.contains(thing.id())) {
				ThingMap thingMap = (ThingMap) InjectionUtils.getInstance(thingMapClass, thing);
				thingMaps.add(thingMap);
			}
		}

		/*
		 * Prepare return
		 */
		if (thingMaps.isEmpty()) {
			throw new ReflectionException("No matching ThingMap found for ConfigChannel [" + channel.address() + "]");
		}

		if (Collection.class.isAssignableFrom(expectedObjectClass)) {
			if (Set.class.isAssignableFrom(expectedObjectClass)) {
				return thingMaps;
			} else if (List.class.isAssignableFrom(expectedObjectClass)) {
				return new ArrayList<>(thingMaps);
			} else {
				throw new ReflectionException("Only List and Set ConfigChannels are currently implemented, not ["
						+ expectedObjectClass + "]. ConfigChannel [" + channel.address() + "]");
			}
		} else {
			// No collection
			if (thingMaps.size() > 1) {
				throw new ReflectionException("Field for ConfigChannel [" + channel.address()
						+ "] is no collection, but more than one ThingMaps [" + thingMaps + "] is fitting for ["
						+ channel.id() + "] in [" + channel.getClass().getSimpleName() + "]");
			} else {
				return thingMaps.iterator().next();
			}
		}

	}
}
