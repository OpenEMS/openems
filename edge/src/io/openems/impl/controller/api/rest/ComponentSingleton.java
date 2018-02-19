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

import org.restlet.Component;
import org.restlet.data.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.exception.OpenemsException;
import io.openems.core.utilities.api.ApiWorker;

public class ComponentSingleton {

	private static Component instance = null;
	private static Integer port = null;

	private final static Logger log = LoggerFactory.getLogger(ComponentSingleton.class);

	protected static synchronized Component getComponent(ConfigChannel<Integer> port, ApiWorker apiWorker) throws OpenemsException {
		if (port.valueOptional().isPresent()) {
			return getComponent(port.valueOptional().get(), apiWorker);
		}
		throw new OpenemsException("Unable to start REST-Api: port is not set");
	}

	protected static synchronized Component getComponent(int port, ApiWorker apiWorker) throws OpenemsException {
		if (ComponentSingleton.instance != null
				&& (ComponentSingleton.port == null || ComponentSingleton.port != port)) {
			// port changed -> restart
			ComponentSingleton.restartComponent(port, apiWorker);
		}
		if (ComponentSingleton.instance == null) {
			// instance not available -> start
			startComponent(port, apiWorker);
		}
		return ComponentSingleton.instance;
	}

	protected static synchronized void restartComponent(int port, ApiWorker apiWorker) throws OpenemsException {
		stopComponent();
		startComponent(port, apiWorker);
	}

	private static synchronized void startComponent(int port, ApiWorker apiWorker) throws OpenemsException {
		ComponentSingleton.instance = new Component();
		ComponentSingleton.instance.getServers().add(Protocol.HTTP, port);
		ComponentSingleton.instance.getDefaultHost().attach("/rest", new RestApiApplication(apiWorker));
		try {
			ComponentSingleton.instance.start();
			ComponentSingleton.port = port;
			log.info("REST-Api started on port [" + port + "].");
		} catch (Exception e) {
			throw new OpenemsException("REST-Api failed on port [" + port + "].", e);
		}
	}

	protected static void stopComponent() {
		if (ComponentSingleton.instance != null) {
			try {
				ComponentSingleton.instance.stop();
				log.error("REST-Api stopped.");
			} catch (Exception e) {
				log.error("REST-Api failed to stop.", e);
			}
			ComponentSingleton.instance = null;
			ComponentSingleton.port = null;
		}
	}
}
