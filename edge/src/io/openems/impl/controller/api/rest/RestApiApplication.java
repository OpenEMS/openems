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
package io.openems.impl.controller.api.rest;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;

import io.openems.core.utilities.api.ApiWorker;
import io.openems.impl.controller.api.rest.internal.OpenemsEnroler;
import io.openems.impl.controller.api.rest.internal.OpenemsVerifier;
import io.openems.impl.controller.api.rest.route.ChannelRestlet;
import io.openems.impl.controller.api.rest.route.DeviceNatureRestlet;
import io.openems.impl.controller.api.rest.route.UserChangePasswordRestlet;

public class RestApiApplication extends Application {

	private final ApiWorker apiWorker;

	public RestApiApplication(ApiWorker apiWorker) {
		this.apiWorker = apiWorker;
	}

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override public synchronized Restlet createInboundRoot() {
		ChallengeAuthenticator guard = createAuthenticator();
		Router router = createRouter();
		guard.setNext(router);
		return guard;
	}

	private ChallengeAuthenticator createAuthenticator() {
		ChallengeAuthenticator guard = new ChallengeAuthenticator(getContext(), ChallengeScheme.HTTP_BASIC,
				"OpenEMS REST-Api");
		guard.setVerifier(new OpenemsVerifier());
		guard.setEnroler(new OpenemsEnroler());
		return guard;
	}

	private Router createRouter() {
		Router router = new Router(getContext());
		// router.attach("/channel/{thing}/{channel}/current", ChannelCurrentResource.class);
		router.attach("/user/changePassword", new UserChangePasswordRestlet());
		router.attach("/channel/{thing}/{channel}", new ChannelRestlet(apiWorker));
		router.attach("/config/deviceNatures", new DeviceNatureRestlet());
		return router;
	}
}
