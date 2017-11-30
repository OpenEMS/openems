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
package io.openems.core.utilities;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.reflect.ClassPath;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.Device;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.thing.Thing;
import io.openems.common.session.Role;
import io.openems.core.ConfigFormat;
import io.openems.core.ThingRepository;

public class ConfigUtils {
	private final static Logger log = LoggerFactory.getLogger(ConfigUtils.class);

	/**
	 * Fill all Config-Channels from a JsonObject configuration
	 *
	 * @param channels
	 * @param jConfig
	 * @throws ConfigException
	 */
	public static void injectConfigChannels(Set<ConfigChannel<?>> channels, JsonObject jConfig, Object... args)
			throws ReflectionException {
		for (ConfigChannel<?> channel : channels) {
			if (!jConfig.has(channel.id()) && (channel.valueOptional().isPresent() || channel.isOptional())) {
				// Element for this Channel is not existing in the configuration, but a default value was set
				continue;
			}
			JsonElement jChannel = JsonUtils.getSubElement(jConfig, channel.id());
			Object parameter = getConfigObject(channel, jChannel, args);
			channel.updateValue(parameter, true);
		}
	}

	/**
	 * Converts an object to a JsonElement
	 *
	 * @param value
	 * @return
	 * @throws NotImplementedException
	 */
	public static JsonElement getAsJsonElement(Object value, ConfigFormat format, Role role) throws NotImplementedException {
		// null
		if (value == null) {
			return null;
		}
		// optional
		if (value instanceof Optional<?>) {
			if (!((Optional<?>) value).isPresent()) {
				return null;
			} else {
				value = ((Optional<?>) value).get();
			}
		}
		try {
			/*
			 * test for simple types
			 */
			return JsonUtils.getAsJsonElement(value);
		} catch (NotImplementedException e) {
			;
		}
		if (value instanceof Thing) {
			/*
			 * type Thing
			 */
			Thing thing = (Thing) value;
			JsonObject j = new JsonObject();
			if (format == ConfigFormat.OPENEMS_UI || !thing.id().startsWith("_")) {
				// ignore generated id names starting with "_"
				j.addProperty("id", thing.id());
				j.addProperty("alias", thing.getAlias());
			}
			// for file-format class is not needed for DeviceNatures
			j.addProperty("class", thing.getClass().getCanonicalName());
			ThingRepository thingRepository = ThingRepository.getInstance();
			for (ConfigChannel<?> channel : thingRepository.getConfigChannels(thing)) {
				if(channel.isReadAllowed(role)) { // check read permissions
					JsonElement jChannel = null;
					jChannel = ConfigUtils.getAsJsonElement(channel, format, role);
					if (jChannel != null) {
						j.add(channel.id(), jChannel);
					}
				}
			}
			// for Bridge: add 'devices' array of thingIds
			if (value instanceof Bridge) {
				Bridge bridge = (Bridge) value;
				JsonArray jDevices = new JsonArray();
				for (Device device : bridge.getDevices()) {
					jDevices.add(device.id());
				}
				j.add("devices", jDevices);
			}
			return j;
		} else if (value instanceof ConfigChannel<?>) {
			/*
			 * type ConfigChannel
			 */
			ConfigChannel<?> channel = (ConfigChannel<?>) value;
			if (!channel.valueOptional().isPresent()) {
				// no value set
				return null;
			} else if (format == ConfigFormat.FILE && channel.getDefaultValue().equals(channel.valueOptional())) {
				// default value not changed
				return null;
			} else {
				// recursive call
				return ConfigUtils.getAsJsonElement(channel.valueOptional().get(), format, role);
			}
		} else if (value instanceof ThingMap) {
			/*
			 * ThingMap (we need only id)
			 */
			return new JsonPrimitive(((ThingMap) value).id());
		} else if (value instanceof List<?>) {
			/*
			 * List
			 */
			JsonArray jArray = new JsonArray();
			for (Object v : (List<?>) value) {
				jArray.add(ConfigUtils.getAsJsonElement(v, format, role));
			}
			return jArray;
		} else if (value instanceof Set<?>) {
			/*
			 * Set
			 */
			JsonArray jArray = new JsonArray();
			for (Object v : (Set<?>) value) {
				jArray.add(ConfigUtils.getAsJsonElement(v, format, role));
			}
			return jArray;
		}
		throw new NotImplementedException("Converter for [" + value + "]" + " of type [" //
				+ value.getClass().getSimpleName() + "]" //
				+ " to JSON is not implemented.");
	}

	/**
	 * Receives a matching value for the ConfigChannel from a JsonElement
	 *
	 * @param channel
	 * @param j
	 * @return
	 * @throws ReflectionException
	 */
	public static Object getConfigObject(ConfigChannel<?> channel, JsonElement j, Object... args)
			throws ReflectionException {
		Optional<Class<?>> typeOptional = channel.type();
		if (!typeOptional.isPresent()) {
			String clazz = channel.parent() != null ? " in implementation [" + channel.parent().getClass() + "]" : "";
			throw new ReflectionException("Type is null for channel [" + channel.address() + "]" + clazz);
		}
		Class<?> type = typeOptional.get();

		/*
		 * test for simple types
		 */
		try {
			return JsonUtils.getAsType(type, j);
		} catch (NotImplementedException e1) {
			;
		}

		if (Thing.class.isAssignableFrom(type)) {
			/*
			 * Asking for a Thing
			 */
			@SuppressWarnings("unchecked") Class<Thing> thingType = (Class<Thing>) type;
			return getThingFromConfig(thingType, j, args);

		} else if (ThingMap.class.isAssignableFrom(type)) {
			/*
			 * Asking for a ThingMap
			 */
			return InjectionUtils.getThingMapsFromConfig(channel, j);

		} else if (Inet4Address.class.isAssignableFrom(type)) {
			/*
			 * Asking for an IPv4
			 */
			try {
				return Inet4Address.getByName(j.getAsString());
			} catch (UnknownHostException e) {
				throw new ReflectionException("Unable to convert [" + j + "] to IPv4 address");
			}
		} else if (Long[].class.isAssignableFrom(type)) {
			/*
			 * Asking for an Array of Long
			 */
			return getLongArrayFromConfig(channel, j);
		}
		throw new ReflectionException("Unable to match config [" + j + "] to class type [" + type + "]");
	}

	private static Thing getThingFromConfig(Class<? extends Thing> type, JsonElement j, Object... objects)
			throws ReflectionException {
		String thingId = JsonUtils.getAsString(j, "id");
		ThingRepository thingRepository = ThingRepository.getInstance();
		Optional<Thing> existingThing = thingRepository.getThingById(thingId);
		Thing thing;
		if (existingThing.isPresent()) {
			// reuse existing Thing
			thing = existingThing.get();
		} else {
			// Thing is not existing. Create a new instance
			Object[] args = new Object[objects.length + 1];
			args[0] = thingId;
			for (int i = 1; i < objects.length + 1; i++) {
				args[i] = objects[i - 1];
			}
			thing = InjectionUtils.getThingInstance(type, args);
			log.debug("Add Thing[" + thing.id() + "], Implementation[" + thing.getClass().getSimpleName() + "]");
			thingRepository.addThing(thing);
		}
		// Recursive call to inject config parameters for the newly created Thing
		injectConfigChannels(thingRepository.getConfigChannels(thing), j.getAsJsonObject());
		// thing.init();
		return thing;
	}

	private static Object getLongArrayFromConfig(ConfigChannel<?> channel, JsonElement j) throws ReflectionException {
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

		if (Collection.class.isAssignableFrom(expectedObjectClass)) {
			if (j.isJsonArray()) {
				Set<Long[]> erg = new HashSet<>();
				for (JsonElement e : j.getAsJsonArray()) {
					if (e.isJsonArray()) {
						JsonArray arr = e.getAsJsonArray();
						Long[] larr = new Long[arr.size()];
						for (int i = 0; i < arr.size(); i++) {
							larr[i] = arr.get(i).getAsLong();
						}
						erg.add(larr);
					} else {
						throw new ReflectionException("The Json object for ConfigChannel [" + channel.address()
						+ "] is no twodimensional array!");
					}
				}
				if (Set.class.isAssignableFrom(expectedObjectClass)) {
					return erg;
				} else if (List.class.isAssignableFrom(expectedObjectClass)) {
					return new ArrayList<>(erg);
				} else {
					throw new ReflectionException("Only List and Set ConfigChannels are currently implemented, not ["
							+ expectedObjectClass + "]. ConfigChannel [" + channel.address() + "]");
				}
			} else {
				throw new ReflectionException(
						"The Json object for ConfigChannel [" + channel.address() + "] is no array!");
			}
		} else {
			if (j.isJsonArray()) {
				JsonArray arr = j.getAsJsonArray();
				Long[] larr = new Long[arr.size()];
				for (int i = 0; i < arr.size(); i++) {
					larr[i] = arr.get(i).getAsLong();
				}
				return larr;
			} else {
				throw new ReflectionException(
						"The Json object for ConfigChannel [" + channel.address() + "] is no array!");
			}
		}
	}

	public static Set<Class<? extends Thing>> getAvailableClasses(String topLevelPackage, Class<? extends Thing> clazz,
			String suffix) throws ReflectionException {
		Set<Class<? extends Thing>> clazzes = new HashSet<>();
		try {
			ClassPath classpath = ClassPath.from(ClassLoader.getSystemClassLoader());
			for (ClassPath.ClassInfo classInfo : classpath.getTopLevelClassesRecursive(topLevelPackage)) {
				if (classInfo.getName().endsWith(suffix)) {
					Class<?> thisClazz = classInfo.load();
					if (clazz.isAssignableFrom(thisClazz)) {
						@SuppressWarnings("unchecked") Class<? extends Thing> thisThingClazz = (Class<? extends Thing>) thisClazz;
						clazzes.add(thisThingClazz);
					}
				}
			}
		} catch (IllegalArgumentException | IOException e) {
			throw new ReflectionException(e.getMessage());
		}
		return clazzes;
	}

	/**
	 * Get all declared members of thing class.
	 *
	 * @param clazz
	 * @return
	 */
	public static List<Member> getMembers(Class<? extends Thing> clazz) {
		List<Member> members = new LinkedList<>();
		for (Method method : clazz.getMethods()) {
			members.add(method);
		}
		for (Field field : clazz.getFields()) {
			members.add(field);
		}
		return Collections.unmodifiableList(members);
	}
}