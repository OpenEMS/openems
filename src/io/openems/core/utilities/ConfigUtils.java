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

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ConfigChannelDoc;
import io.openems.api.doc.ThingDoc;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ConfigException;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.thing.Thing;
import io.openems.core.ClassRepository;
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
	public static void injectConfigChannels(Set<ConfigChannel<?>> channels, JsonObject jConfig)
			throws ReflectionException {
		for (ConfigChannel<?> channel : channels) {
			if (!jConfig.has(channel.id()) && (channel.valueOptional().isPresent() || channel.isOptional())) {
				// Element for this Channel is not existing existing in the configuration, but a default value was set
				continue;
			}
			JsonElement jChannel = JsonUtils.getSubElement(jConfig, channel.id());
			Object parameter = getConfigObject(channel, jChannel);
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
	public static JsonElement getAsJsonElement(Object value) throws NotImplementedException {
		return getAsJsonElement(value, false);
	}

	/**
	 * Converts an object to a JsonElement
	 *
	 * @param value
	 * @return
	 * @throws NotImplementedException
	 */
	public static JsonElement getAsJsonElement(Object value, boolean includeEverything) throws NotImplementedException {
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
			if (includeEverything || !thing.id().startsWith("_")) {
				// ignore generated id names starting with "_"
				j.addProperty("id", thing.id());
			}
			if (!(value instanceof DeviceNature)) {
				// class is not needed for DeviceNatures
				j.addProperty("class", thing.getClass().getCanonicalName());
			}
			ThingRepository thingRepository = ThingRepository.getInstance();
			for (ConfigChannel<?> channel : thingRepository.getConfigChannels(thing)) {
				JsonElement jChannel = ConfigUtils.getAsJsonElement(channel, includeEverything);
				if (jChannel != null) {
					j.add(channel.id(), jChannel);
				}
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
			} else if (!includeEverything && channel.getDefaultValue().equals(channel.valueOptional())) {
				// default value not changed
				return null;
			} else {
				// recursive call
				return ConfigUtils.getAsJsonElement(channel.valueOptional().get(), includeEverything);
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
				jArray.add(ConfigUtils.getAsJsonElement(v, includeEverything));
			}
			return jArray;
		} else if (value instanceof Set<?>) {
			/*
			 * Set
			 */
			JsonArray jArray = new JsonArray();
			for (Object v : (Set<?>) value) {
				jArray.add(ConfigUtils.getAsJsonElement(v, includeEverything));
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
	private static Object getConfigObject(ConfigChannel<?> channel, JsonElement j) throws ReflectionException {
		Class<?> type = channel.type();

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
			return getThingFromConfig((Class<Thing>) type, j);

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

	private static Thing getThingFromConfig(Class<? extends Thing> type, JsonElement j) throws ReflectionException {
		String thingId = JsonUtils.getAsString(j, "id");
		ThingRepository thingRepository = ThingRepository.getInstance();
		Optional<Thing> existingThing = thingRepository.getThingById(thingId);
		Thing thing;
		if (existingThing.isPresent()) {
			// reuse existing Thing
			thing = existingThing.get();
		} else {
			// Thing is not existing. Create a new instance
			thing = InjectionUtils.getThingInstance(type, thingId);
			log.debug("Add Thing[" + thing.id() + "], Implementation[" + thing.getClass().getSimpleName() + "]");
			thingRepository.addThing(thing);
		}
		// Recursive call to inject config parameters for the newly created Thing
		injectConfigChannels(thingRepository.getConfigChannels(thing), j.getAsJsonObject());
		thing.init();
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

	public static ThingDoc getThingDescription(Class<? extends Thing> clazz) {
		ThingDoc doc = new ThingDoc(clazz);

		ThingInfo thing = clazz.getAnnotation(ThingInfo.class);
		if (thing == null) {
			log.warn("Thing [" + clazz.getName() + "] has no @Thing annotation");
		} else {
			doc.setThingDescription(thing);
		}
		ClassRepository classRepository = ClassRepository.getInstance();
		classRepository.getThingConfigChannels(clazz).forEach((member, config) -> {
			doc.addConfigChannel(
					new ConfigChannelDoc(member.getName(), config.title(), config.type(), config.isOptional()));
		});
		return doc;
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
						clazzes.add((Class<? extends Thing>) thisClazz);
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
