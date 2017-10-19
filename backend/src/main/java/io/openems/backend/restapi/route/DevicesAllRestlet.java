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
package io.openems.backend.restapi.route;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import com.google.gson.JsonArray;

import io.openems.backend.openemswebsocket.OpenemsWebsocket;
import io.openems.backend.openemswebsocket.session.OpenemsSession;

public class DevicesAllRestlet extends Restlet {

	@Override
	public void handle(Request request, Response response) {
		super.handle(request, response);

		// call handler methods
		if (request.getMethod().equals(Method.GET)) {
			JsonArray j = new JsonArray();
			for (OpenemsSession session : OpenemsWebsocket.instance().getSessions()) {
				j.add(session.getData().getDevices().toJson());
			}
			Representation entity = new StringRepresentation(j.toString(), MediaType.APPLICATION_JSON);
			response.setEntity(entity);
		}
	}
}
