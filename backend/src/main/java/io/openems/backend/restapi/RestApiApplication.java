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
package io.openems.backend.restapi;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

import io.openems.backend.restapi.route.DevicesAllRestlet;

public class RestApiApplication extends Application {

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override
	public synchronized Restlet createInboundRoot() {
		// ChallengeAuthenticator guard = createAuthenticator();
		Router router = createRouter();
		// guard.setNext(router);
		// return guard;
		return router;
	}

	// TODO authentication
	// private ChallengeAuthenticator createAuthenticator() {
	// ChallengeAuthenticator guard = new ChallengeAuthenticator(getContext(), ChallengeScheme.HTTP_BASIC,
	// "OpenEMS REST-Api");
	// guard.setVerifier(new OpenemsVerifier());
	// guard.setEnroler(new OpenemsEnroler());
	// return guard;
	// }

	private Router createRouter() {
		Router router = new Router(getContext());
		router.attach("/devices/all", new DevicesAllRestlet());
		// router.attach("/device/{deviceId}", new ChannelRestlet());
		return router;
	}
}
