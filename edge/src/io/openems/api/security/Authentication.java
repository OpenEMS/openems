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
package io.openems.api.security;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Authentication {
	private final static Logger log = LoggerFactory.getLogger(Authentication.class);

	/*
	 * Singleton
	 */
	private static Authentication instance;

	public static synchronized Authentication getInstance() {
		if (Authentication.instance == null) {
			Authentication.instance = new Authentication();
		}
		return Authentication.instance;
	}

	/*
	 * This class
	 */
	private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

	private Authentication() {}

	public Session byUserPassword(String username, String password) {
		User user = User.authenticate(username, password);
		Session session = addSession(user);
		if (session != null) {
			log.info("User[" + session.getUser().getName() + "] authenticated. " + //
					"Created session[" + session.getToken() + "].");
		} else {
			log.info("Authentication by username[" + username + "] + password failed.");
		}
		return session;
	}

	public Session byPassword(String password) {
		User user = User.authenticate(password);
		Session session = addSession(user);
		if (session != null) {
			log.info("User[" + session.getUser().getName() + "] authenticated. " + //
					"Created session[" + session.getToken() + "].");
		} else {
			log.info("Authentication by password failed.");
		}
		return session;
	}

	public Session bySession(String token) {
		Session session = sessions.get(token);
		if (session != null) {
			log.info("User[" + session.getUser().getName() + "] authenticated by " + //
					"session[" + session.getToken() + "].");
		} else {
			log.info("Authentication by session failed.");
		}
		return session;
	}

	private Session addSession(User user) {
		if (user != null) {
			Session session = new Session(user);
			sessions.put(session.getToken(), session);
			return session;
		}
		return null;
	}
}
