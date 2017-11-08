package io.openems.core.utilities;

import java.net.Inet4Address;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.NotImplementedException;

/**
 * All types that are used somewhere in the system. This helps with type casting and reflection in certain classes, as
 * an incomplete "switch" on an Enum will trigger a warning - as long as there is no "default" branch.
 *
 * @author stefan.feilmeier
 *
 */
public enum OpenemsTypes {
	/*
	 * Primitives
	 */
	SHORT, INTEGER, LONG, DOUBLE, BOOLEAN, STRING, //
	/*
	 * Arrays of primitives
	 */
	LONG_ARRAY,
	/*
	 * Complex types
	 */
	INET_4_ADDRESS,
	/*
	 * Json
	 */
	JSON_ARRAY, JSON_OBJECT,
	/*
	 * Things
	 */
	DEVICE_NATURE, THING_MAP;

	public static OpenemsTypes get(Class<?> type) throws NotImplementedException {
		if (Short.class.isAssignableFrom(type)) {
			return SHORT;

		} else if (Integer.class.isAssignableFrom(type)) {
			return INTEGER;

		} else if (Long.class.isAssignableFrom(type)) {
			return LONG;

		} else if (Double.class.isAssignableFrom(type)) {
			return DOUBLE;

		} else if (Boolean.class.isAssignableFrom(type)) {
			return BOOLEAN;

		} else if (String.class.isAssignableFrom(type)) {
			return STRING;

		} else if (Long[].class.isAssignableFrom(type)) {
			return LONG_ARRAY;

		} else if (Inet4Address.class.isAssignableFrom(type)) {
			return INET_4_ADDRESS;

		} else if (JsonArray.class.isAssignableFrom(type)) {
			return JSON_ARRAY;

		} else if (JsonObject.class.isAssignableFrom(type)) {
			return JSON_OBJECT;

		} else if (DeviceNature.class.isAssignableFrom(type)) {
			return DEVICE_NATURE;

		} else if (ThingMap.class.isAssignableFrom(type)) {
			return THING_MAP;
		}
		throw new NotImplementedException("Type [" + type + "] is not defined as OpenemsType.");
	}

}
