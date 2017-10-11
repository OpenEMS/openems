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
package io.openems.impl.controller.api.rest.internal;

import org.restlet.Application;
import org.restlet.data.ClientInfo;
import org.restlet.security.Enroler;
import org.restlet.security.Role;

import io.openems.api.exception.OpenemsException;
import io.openems.api.security.User;

public class OpenemsEnroler implements Enroler {

	@Override
	public void enrole(ClientInfo clientInfo) {
		String username = clientInfo.getUser().getIdentifier();
		User user;
		try {
			user = User.getUserByName(username);
			clientInfo.getRoles().add( //
					Role.get(Application.getCurrent(), user.getRole().name().toLowerCase()) //
			);
		} catch (OpenemsException e) { /* ignore, just don't enrole user in any group */ }
	}
}
