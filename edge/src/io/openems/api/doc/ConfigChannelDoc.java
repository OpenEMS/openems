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
package io.openems.api.doc;

import com.google.gson.JsonObject;

import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.security.User;
import io.openems.core.utilities.InjectionUtils;

public class ConfigChannelDoc {
	private final String name;
	private final String title;
	private final Class<?> type;
	private final boolean optional;
	private final boolean array;
	private final User accessLevel;

	public ConfigChannelDoc(String name, String title, Class<?> type, boolean optional, boolean array,
			User accessLevel) {
		this.name = name;
		this.title = title;
		this.type = type;
		this.optional = optional;
		this.array = array;
		this.accessLevel = accessLevel;
	}

	public String getName() {
		return name;
	}

	public JsonObject getAsJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("name", name);
		j.addProperty("title", title);
		if (ThingMap.class.isAssignableFrom(type)) {
			// for ThingMap type: get the types from annotation and return JsonArray
			IsThingMap isThingMapAnnotation = type.getAnnotation(IsThingMap.class);
			j.add("type", InjectionUtils.getImplementsAsJson(isThingMapAnnotation.type()));
		} else {
			// for simple types, use only simple name (e.g. 'Long', 'Integer',...)
			j.addProperty("type", type.getSimpleName());
		}
		j.addProperty("optional", optional);
		j.addProperty("array", array);
		j.addProperty("accessLevel", accessLevel.name().toLowerCase());
		return j;
	}
}
