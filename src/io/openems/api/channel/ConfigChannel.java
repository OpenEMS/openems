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
package io.openems.api.channel;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.NotImplementedException;
import io.openems.api.exception.OpenemsException;
import io.openems.api.thing.Thing;
import io.openems.core.utilities.InjectionUtils;
import io.openems.core.utilities.JsonUtils;

public class ConfigChannel<T> extends WriteChannel<T> {
	private Class<?> type;
	private Optional<T> defaultValue = Optional.empty();
	private boolean isOptional;

	public ConfigChannel(String id, Thing parent) {
		super(id, parent);
	}

	/**
	 * Sets values for this ConfigChannel using its annotation
	 *
	 * This method is called by reflection from {@link InjectionUtils.getThingInstance}
	 *
	 * @param parent
	 * @throws OpenemsException
	 */
	public void applyAnnotation(ConfigInfo configAnnotation) throws OpenemsException {
		this.type = configAnnotation.type();
		this.isOptional = configAnnotation.isOptional();
		if (!configAnnotation.defaultValue().isEmpty()) {
			JsonElement jValue = null;
			try {
				jValue = (new JsonParser()).parse(configAnnotation.defaultValue());
				this.defaultValue((T) JsonUtils.getAsType(type, jValue));
			} catch (NotImplementedException | JsonSyntaxException e) {
				throw new OpenemsException("Unable to set defaultValue [" + jValue + "] " + e.getMessage());
			}
		}
	}

	@Override
	public ConfigChannel<T> addUpdateListener(ChannelUpdateListener... listeners) {
		return (ConfigChannel<T>) super.addUpdateListener(listeners);
	}

	@Override
	public ConfigChannel<T> addChangeListener(ChannelChangeListener... listeners) {
		return (ConfigChannel<T>) super.addChangeListener(listeners);
	}

	public Class<?> type() {
		return this.type;
	}

	@Override
	public void updateValue(Object value, boolean triggerEvent) {
		super.updateValue((T) value, triggerEvent);
	}

	public void updateValue(JsonElement jValue, boolean triggerEvent) throws NotImplementedException {
		T value = (T) JsonUtils.getAsType(type, jValue);
		this.updateValue(value, triggerEvent);
	}

	public ConfigChannel<T> defaultValue(T value) {
		this.defaultValue = Optional.ofNullable(value);
		updateValue(value, false);
		return this;
	}

	public Optional<T> getDefaultValue() {
		return this.defaultValue;
	}

	// TODO: remove, obsolete
	private ConfigChannel<T> optional() {
		this.isOptional = true;
		return this;
	}

	public boolean isOptional() {
		return isOptional;
	}

	@Override
	public JsonObject toJsonObject() throws NotImplementedException {
		JsonObject j = super.toJsonObject();
		j.addProperty("writeable", true);
		return j;
	}

	@Override
	public String toString() {
		return "ConfigChannel[" + this.valueOptional().toString() + "]";
	}
}
