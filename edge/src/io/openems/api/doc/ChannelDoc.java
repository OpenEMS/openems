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

import java.lang.reflect.Member;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.security.User;
import io.openems.core.utilities.BitUtils;
import io.openems.core.utilities.InjectionUtils;

public class ChannelDoc {
	private final Logger log = LoggerFactory.getLogger(ChannelDoc.class);

	private final Member member;
	private final String name;
	private final String title;
	private final String description;
	private final Optional<Class<?>> typeOpt;
	// number of bits required for this datatype
	private final Optional<Integer> bitLengthOpt;
	private final boolean optional;
	private final boolean array;
	private final User accessLevel;
	private final String defaultValue;

	public ChannelDoc(Member member, String name, Optional<ChannelInfo> channelInfoOpt) {
		this.member = member;
		this.name = name;
		if (channelInfoOpt.isPresent()) {
			ChannelInfo channelInfo = channelInfoOpt.get();
			this.title = channelInfo.title();
			this.description = channelInfo.description();
			this.typeOpt = Optional.of(channelInfo.type());
			Integer bitLength = null;
			try {
				bitLength = BitUtils.getBitLength(channelInfo.type());
			} catch (NotImplementedException e) {
				log.warn("Unable to get BitLength for Channel [" + name + "]: " + e.getMessage());
			}
			this.bitLengthOpt = Optional.ofNullable(bitLength);
			this.optional = channelInfo.isOptional();
			this.array = channelInfo.isArray();
			this.accessLevel = channelInfo.accessLevel();
			this.defaultValue = channelInfo.defaultValue();
		} else {
			this.title = ChannelInfo.DEFAULT_TITLE;
			this.description = ChannelInfo.DEFAULT_DESCRIPTION;
			this.typeOpt = Optional.empty();
			this.bitLengthOpt = Optional.empty();
			this.optional = ChannelInfo.DEFAULT_IS_OPTIONAL;
			this.array = ChannelInfo.DEFAULT_IS_ARRAY;
			this.accessLevel = ChannelInfo.DEFAULT_ACCESS_LEVEL;
			this.defaultValue = ChannelInfo.DEFAULT_VALUE;
		}
	}

	public Member getMember() {
		return member;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Optional<Class<?>> getTypeOpt() {
		return typeOpt;
	}

	public Optional<Integer> getBitLengthOpt() {
		return bitLengthOpt;
	}

	public boolean isOptional() {
		return optional;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public JsonObject getAsJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("name", this.name);
		j.addProperty("title", this.title);
		j.addProperty("description", this.description);
		if (typeOpt.isPresent()) {
			Class<?> type = this.typeOpt.get();
			if (ThingMap.class.isAssignableFrom(type)) {
				// for ThingMap type: get the types from annotation and return JsonArray
				IsThingMap isThingMapAnnotation = type.getAnnotation(IsThingMap.class);
				j.add("type", InjectionUtils.getImplementsAsJson(isThingMapAnnotation.type()));
			} else if (DeviceNature.class.isAssignableFrom(type)) {
				// for DeviceNatures add complete class name
				j.addProperty("type", type.getCanonicalName());
			} else {
				// for simple types, use only simple name (e.g. 'Long', 'Integer',...)
				j.addProperty("type", type.getSimpleName());
			}
		}
		j.addProperty("optional", this.optional);
		j.addProperty("array", this.array);
		j.addProperty("accessLevel", this.accessLevel.name().toLowerCase());
		j.addProperty("defaultValue", this.defaultValue);
		return j;
	}
}
