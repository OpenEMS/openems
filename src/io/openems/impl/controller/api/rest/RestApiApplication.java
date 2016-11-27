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
import org.restlet.security.MemoryRealm;
import org.restlet.security.Role;
import org.restlet.security.User;

import io.openems.api.security.OpenemsRole;

public class RestApiApplication extends Application {

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
		ChallengeAuthenticator guard = new ChallengeAuthenticator(getContext(), ChallengeScheme.HTTP_BASIC, "realm");

		// Create in-memory users with roles
		MemoryRealm realm = new MemoryRealm();
		// TODO : read from configuration
		User admin = new User("admin", "admin");
		realm.getUsers().add(admin);
		realm.map(admin, Role.get(this, OpenemsRole.ADMIN.toString()));
		User installer = new User("installer", "installer");
		realm.getUsers().add(installer);
		realm.map(installer, Role.get(this, OpenemsRole.INSTALLER.toString()));
		User owner = new User("owner", "owner");
		realm.getUsers().add(owner);
		realm.map(owner, Role.get(this, OpenemsRole.OWNER.toString()));
		User user = new User("user", "user");
		realm.getUsers().add(user);
		realm.map(user, Role.get(this, OpenemsRole.USER.toString()));

		// Attach verifier to check authentication and enroler to determine roles
		guard.setVerifier(realm.getVerifier());
		guard.setEnroler(realm.getEnroler());
		return guard;
	}

	private Router createRouter() {
		Router router = new Router(getContext());
		// router.attach("/channel/{thing}/{channel}/current", ChannelCurrentResource.class);
		router.attach("/channel/{thing}/{channel}", new ChannelRestlet());
		return router;
	}
}
