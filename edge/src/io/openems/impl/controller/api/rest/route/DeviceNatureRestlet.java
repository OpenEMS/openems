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
package io.openems.impl.controller.api.rest.route;

import java.util.Set;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.api.device.nature.DeviceNature;
import io.openems.api.thing.Thing;
import io.openems.core.ThingRepository;
import io.openems.impl.controller.api.rest.OpenemsRestlet;

public class DeviceNatureRestlet extends OpenemsRestlet {

	private final ThingRepository thingRepository;

	public DeviceNatureRestlet() {
		super();
		thingRepository = ThingRepository.getInstance();
	}

	@Override public void handle(Request request, Response response) {
		super.handle(request, response);

		// call handler methods
		if (request.getMethod().equals(Method.GET)) {
			Representation entity = get();
			response.setEntity(entity);
		}
	}

	/**
	 * handle HTTP GET request
	 *
	 * @param thingId
	 * @param channelId
	 * @return
	 */
	private Representation get() {
		JsonObject j = new JsonObject();
		for (Class<? extends Thing> clazz : thingRepository.getThingClasses()) {
			if (DeviceNature.class.isAssignableFrom(clazz)) {
				// clazz is a DeviceNature
				Set<Thing> things = thingRepository.getThingsByClass(clazz);
				JsonArray jThings = new JsonArray();
				for (Thing thing : things) {
					jThings.add(thing.id());
				}
				j.add(clazz.getCanonicalName(), jThings);
			}
		}
		return new StringRepresentation(j.toString(), MediaType.APPLICATION_JSON);
	}
}
