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
package io.openems.api.thing;

public interface Thing {

	public String id();

	public default String getAlias() {
		return id();
	}

	public default void addListener(ThingChannelsUpdatedListener listener) {

	}

	public default void removeListener(ThingChannelsUpdatedListener listener) {

	}

	public default void init() {

	}

	/**
	 * Sets the Thing annotation. This method is called after the thing was initialized via init()
	 *
	 * @throws OpenemsException
	 */
	// TODO public void setThingDoc(ThingDoc channelDoc) throws OpenemsException;
}
