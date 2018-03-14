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

import java.util.Set;

import com.google.gson.JsonObject;

import io.openems.api.doc.ChannelDoc;
import io.openems.api.thing.Thing;
import io.openems.common.exceptions.AccessDeniedException;
import io.openems.common.exceptions.NotImplementedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;

public interface Channel {
	public String id();

	public Thing parent();

	/**
	 * Gets the channel address for this Channel (e.g. "ess0/Soc")
	 *
	 * @return
	 */
	public ChannelAddress address();

	/**
	 * Register a listener for update events on this Channel
	 *
	 * @param listeners
	 * @return itself
	 */
	public Channel addUpdateListener(ChannelUpdateListener... listeners);

	/**
	 * Register a listener for change events on this Channel
	 *
	 * @param listeners
	 * @return itself
	 */
	public Channel addChangeListener(ChannelChangeListener... listeners);

	/**
	 * Remove a listener for update events on this Channel
	 *
	 * @param listeners
	 * @return itself
	 */
	public Channel removeUpdateListener(ChannelUpdateListener... listeners);

	/**
	 * Remove a listener for change events on this Channel
	 *
	 * @param listeners
	 * @return itself
	 */
	public Channel removeChangeListener(ChannelChangeListener... listeners);

	/**
	 * Convert the channel to a JsonObject
	 *
	 * @return
	 * @throws NotImplementedException
	 */
	public JsonObject toJsonObject() throws NotImplementedException;

	/**
	 * Returns Roles that have read access to this Channel.
	 *
	 * @return
	 */
	public Set<Role> readRoles();

	/**
	 * Is the given Role allowed to read this Channel?
	 *
	 * @param role
	 * @return
	 */
	public boolean isReadAllowed(Role role);

	/**
	 * Is the given Role allowed to read this Channel? Throws AccessDeniedException if not.
	 *
	 * @param role
	 */
	public void assertReadAllowed(Role role) throws AccessDeniedException;

	/**
	 * Returns Roles that have write access to this Channel.
	 *
	 * @return
	 */
	public Set<Role> writeRoles();

	/**
	 * Is the given Role allowed to write this Channel?
	 *
	 * @param role
	 * @return
	 */
	public boolean isWriteAllowed(Role role);

	/**
	 * Is the given Role allowed to write this Channel? Throws AccessDeniedException if not.
	 *
	 * @param role
	 */
	public void assertWriteAllowed(Role role) throws AccessDeniedException;

	/**
	 * Sets the Channel annotation. This method is called twice. Once after creating the Thing and after the thing was
	 * initialized via init()
	 *
	 * @throws OpenemsException
	 */
	public void setChannelDoc(ChannelDoc channelDoc) throws OpenemsException;
}
