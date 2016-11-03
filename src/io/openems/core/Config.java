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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.IsThingMapping;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.Device;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.exception.WriteChannelException;
import io.openems.api.scheduler.Scheduler;
import io.openems.api.thing.Thing;
import io.openems.core.utilities.InjectionUtils;
import io.openems.core.utilities.JsonUtils;

public class Config {
	private final static Logger log = LoggerFactory.getLogger(Config.class);

	public void readConfig(JsonObject jConfig) throws ReflectionException, ConfigException, WriteChannelException {
		ThingRepository thingRepository = ThingRepository.getInstance();
		/*
		 * read each Bridge in "things" array
		 */
		JsonArray jThings = JsonUtils.getAsJsonArray(jConfig, "things");
		for (JsonElement jBridgeElement : jThings) {
			JsonObject jBridge = JsonUtils.getAsJsonObject(jBridgeElement);
			String bridgeClass = JsonUtils.getAsString(jBridge, "class");
			Bridge bridge = (Bridge) InjectionUtils.getThingInstance(bridgeClass);
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
		injectConfigChannels(thingRepository.getConfigChannels(scheduler), jScheduler);
		/*
		 * read each Controller in "controllers" array
		 */
		JsonArray jControllers = JsonUtils.getAsJsonArray(jScheduler, "controllers");
		for (JsonElement jControllerElement : jControllers) {
			JsonObject jController = JsonUtils.getAsJsonObject(jControllerElement);
			String controllerClass = JsonUtils.getAsString(jController, "class");
			Controller controller = (Controller) InjectionUtils.getThingInstance(controllerClass);
			injectConfigChannels(thingRepository.getConfigChannels(controller), jController);
			injectControllerNatureMapping(controller);
			scheduler.addController(controller);
		}
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
			if (!jConfig.has(channel.id()) && channel.valueOptional().isPresent()) {
				// Element for this Channel is not existing existing in the configuration, but a default value was set
				continue;
			}
			JsonElement jConfigElement = JsonUtils.getSubElement(jConfig, channel.id());
			if (jConfigElement.isJsonPrimitive()) {
				/**
				 * Parameter is a JsonPrimitive
				 */
				JsonPrimitive jConfigParameter = jConfigElement.getAsJsonPrimitive();
				Object object = JsonUtils.getJsonPrimitiveAsClass(jConfigParameter, channel.type());
				channel.updateValue(object, true);
			} else {
				/**
				 * Parameter is NOT a JsonPrimitive -> create a matching Thing
				 */
				JsonObject jConfigParameter = JsonUtils.getAsJsonObject(jConfigElement);
				String thingId = JsonUtils.getAsString(jConfigParameter, "id");
				ThingRepository thingRepository = ThingRepository.getInstance();
				Optional<Thing> existingThing = thingRepository.getThingById(thingId);
				Thing thing;
				if (existingThing.isPresent()) {
					// reuse existing Thing
					thing = existingThing.get();
				} else {
					// Thing is not existing. Create a new instance
					thing = InjectionUtils.getThingInstance(channel.type(), thingId);
				}
				// Recursive call to inject config parameters for the newly created Thing
				injectConfigChannels(thingRepository.getConfigChannels(thing), jConfigParameter);
				channel.updateValue(thing, true);
			}

		}

	}

	private enum InjectionType {
		OBJECT, SET, LIST
	}

	private void injectControllerNatureMapping(Controller controller) throws ReflectionException {
		ThingRepository thingRepository = ThingRepository.getInstance();
		for (Field field : controller.getClass().getDeclaredFields()) {
			IsThingMapping annotation = field.getAnnotation(IsThingMapping.class);
			if (annotation != null) {
				// field is annotated with @IsThingMapping

				// marker to tell if only one mapped Thing or a list of Things is expected
				InjectionType expected = InjectionType.OBJECT;

				/*
				 * Get the ThingMap class
				 */
				Class<? extends ThingMap> thingMapClass;
				try {
					thingMapClass = (Class<? extends ThingMap>) field.getType();
					if (Set.class.isAssignableFrom(thingMapClass)) {
						// Field is a set. Get the generic type
						ParameterizedType type = (ParameterizedType) field.getGenericType();
						thingMapClass = (Class<? extends ThingMap>) Class
								.forName(type.getActualTypeArguments()[0].getTypeName());
						expected = InjectionType.SET;
					} else if (List.class.isAssignableFrom(thingMapClass)) {
						// Field is a list. Get the generic type
						ParameterizedType type = (ParameterizedType) field.getGenericType();
						thingMapClass = (Class<? extends ThingMap>) Class
								.forName(type.getActualTypeArguments()[0].getTypeName());
						expected = InjectionType.LIST;
					}
				} catch (ClassNotFoundException e) {
					throw new ReflectionException("Unable to find ThingMap [" + annotation + "].");
				}

				/*
				 * Get the referenced Thing class
				 */
				if (!thingMapClass.isAnnotationPresent(IsThingMap.class)) {
					throw new ReflectionException("ThingMap [" + thingMapClass.getSimpleName()
							+ "] has no defined target Thing! 'IsThingMap'-annotation is missing.");
				}
				IsThingMap isThingMap = thingMapClass.getAnnotation(IsThingMap.class);
				Class<? extends Thing> thingClass = isThingMap.type();

				/*
				 * Create ThingMap instance(s) for each matching Thing
				 */
				Set<Thing> matchingThings = thingRepository.getThingsAssignableByClass(thingClass);
				Set<ThingMap> thingMaps = new HashSet<>();
				for (Thing thing : matchingThings) {
					ThingMap thingMap = (ThingMap) InjectionUtils.getInstance(thingMapClass, thing);
					thingMaps.add(thingMap);
					if (expected == InjectionType.OBJECT) {
						break;
					}
				}

				/*
				 * If a matching ThingMap was created:
				 * ThingMap -> Controller.@IsThingMapping
				 */
				if (!thingMaps.isEmpty()) {
					try {
						if (expected == InjectionType.SET) {
							field.set(controller, thingMaps);
						} else if (expected == InjectionType.LIST) {
							field.set(controller, new ArrayList<>(thingMaps));
						} else {
							field.set(controller, thingMaps.iterator().next());
						}
					} catch (IllegalArgumentException | IllegalAccessException e) {
						throw new ReflectionException(
								"Unable to set IsThingMapping to Field [" + controller.getClass().getSimpleName() + "."
										+ field.getName() + "]: " + e.getMessage());
					}
				}
			}
		}

	}
}
