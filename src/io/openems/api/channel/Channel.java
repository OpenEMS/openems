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
package io.openems.api.channel;

import com.google.gson.JsonObject;

import io.openems.api.exception.NotImplementedException;
import io.openems.api.thing.Thing;

public interface Channel {
	public String id();

	public Thing parent();

	public String address();

	/**
	 * Register a listener for update events on this Channel
	 *
	 * @param listeners
	 * @return itself
	 */
	public Channel updateListener(ChannelUpdateListener... listeners);

	/**
	 * Register a listener for change events on this Channel
	 *
	 * @param listeners
	 * @return itself
	 */
	public Channel changeListener(ChannelChangeListener... listeners);

	/**
	 * Convert the channel to a JsonObject
	 *
	 * @return
	 * @throws NotImplementedException
	 */
	public JsonObject toJsonObject() throws NotImplementedException;
}
