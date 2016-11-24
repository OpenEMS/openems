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
package io.openems.impl.api.rest;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.routing.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestApi extends Application {

	private static Logger log = LoggerFactory.getLogger(RestApi.class);

	public final static int DEFAULT_PORT = 8084;
	private static Component component;

	public static synchronized Component startComponent() throws Exception {
		return RestApi.startComponent(DEFAULT_PORT);
	}

	public static synchronized Component startComponent(int port) throws Exception {
		if (RestApi.component == null) {
			RestApi.component = new Component();
			RestApi.component.getServers().add(Protocol.HTTP, port);
			RestApi.component.getDefaultHost().attach("/rest", new RestApi());
			RestApi.component.start();
		}
		return RestApi.component;
	}

	public static void stopComponent() throws Exception {
		if (RestApi.component != null) {
			RestApi.component.stop();
			RestApi.component = null;
		}
	}

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override public synchronized Restlet createInboundRoot() {
		Router router = new Router(getContext());
		// define all routes
		router.attach("/channel/{thing}/{channel}/current", ChannelCurrentResource.class);

		return router;
	}
}
