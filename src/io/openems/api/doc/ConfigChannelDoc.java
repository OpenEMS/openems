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

public class ConfigChannelDoc {
	private final String name;
	private final String title;
	private final Class<?> type;
	private final boolean optional;

	public ConfigChannelDoc(String name, String title, Class<?> type, boolean optional) {
		this.name = name;
		this.title = title;
		this.type = type;
		this.optional = optional;
	}

	public JsonObject getAsJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("name", name);
		j.addProperty("title", title);
		j.addProperty("type", type.getSimpleName());
		j.addProperty("optional", optional);
		return j;
	}
}
