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
package io.openems.impl.controller.api.rest;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;

@ThingInfo("REST-API (z. B. für externe Datenabfrage)")
public class RestApiController extends Controller {

	public RestApiController() {
		super();
	}

	public RestApiController(String thingId) {
		super(thingId);
	}

	@ConfigInfo(title = "Sets the port", type = Integer.class)
	public final ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this).defaultValue(8084);

	@Override
	public void run() {
		// Start REST-Api server
		try {
			ComponentSingleton.getComponent(port);
		} catch (OpenemsException e) {
			log.error(e.getMessage() + ": " + e.getCause());
		}
	}
}
