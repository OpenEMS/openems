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
package io.openems.core.utilities;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.api.exception.ReflectionException;

public class JsonUtils {
	public static JsonArray getAsJsonArray(JsonElement jElement, String memberName) throws ReflectionException {
		JsonElement jSubElement = getSubElement(jElement, memberName);
		if (!jSubElement.isJsonArray()) {
			throw new ReflectionException("Config [" + memberName + "] is not a JsonArray: " + jSubElement);
		}
		return jSubElement.getAsJsonArray();
	};

	public static JsonObject getAsJsonObject(JsonElement jElement) throws ReflectionException {
		if (!jElement.isJsonObject()) {
			throw new ReflectionException("Config is not a JsonObject: " + jElement);
		}
		return jElement.getAsJsonObject();
	};

	public static JsonObject getAsJsonObject(JsonElement jElement, String memberName) throws ReflectionException {
		JsonElement jsubElement = getSubElement(jElement, memberName);
		if (!jsubElement.isJsonObject()) {
			throw new ReflectionException("Config is not a JsonObject: " + jsubElement);
		}
		return jsubElement.getAsJsonObject();
	};

	public static JsonPrimitive getAsPrimitive(JsonElement jElement, String memberName) throws ReflectionException {
		JsonElement jSubElement = getSubElement(jElement, memberName);
		if (!jSubElement.isJsonPrimitive()) {
			throw new ReflectionException("Config is not a JsonPrimitive: " + jSubElement);
		}
		return jSubElement.getAsJsonPrimitive();
	}

	public static String getAsString(JsonElement jElement, String memberName) throws ReflectionException {
		JsonPrimitive jPrimitive = getAsPrimitive(jElement, memberName);
		if (!jPrimitive.isString()) {
			throw new ReflectionException("[" + memberName + "] is not a String: " + jPrimitive);
		}
		return jPrimitive.getAsString();
	}

	public static Object getJsonPrimitiveAsClass(JsonPrimitive j, Class<?> clazz) throws ReflectionException {
		Object parameter = null;
		if (j.isNumber()) {
			if (clazz.isAssignableFrom(Integer.class)) {
				parameter = j.getAsInt();
			}
		} else if (j.isString()) {
			if (clazz.isAssignableFrom(String.class)) {
				parameter = j.getAsString();
			} else if (clazz.isAssignableFrom(Inet4Address.class)) {
				try {
					parameter = Inet4Address.getByName(j.getAsString());
				} catch (UnknownHostException e) {
					throw new ReflectionException("Unable to convert [" + j + "] to IPv4 address");
				}
			}
		}
		if (parameter == null) {
			throw new ReflectionException("Unable to match config [" + j + "] to class type [" + clazz + "]");
		}
		return parameter;
	}

	public static JsonElement getSubElement(JsonElement jElement, String memberName) throws ReflectionException {
		JsonObject jObject = getAsJsonObject(jElement);
		if (!jObject.has(memberName)) {
			throw new ReflectionException("[" + memberName + "] is missing in Config: " + jElement);
		}
		return jObject.get(memberName);
	}

}
