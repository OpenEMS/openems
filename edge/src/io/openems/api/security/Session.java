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

import java.math.BigInteger;
import java.security.SecureRandom;

import io.openems.common.utils.SecureRandomSingleton;

public class Session {

	private final static int SESSION_ID_LENGTH = 130;

	private final String token;
	private final User user;
	// TODO: timeout, IP address,...

	public Session(String token, User user) {
		this.token = token;
		this.user = user;
	}

	public Session(User user) {
		this(generateSessionId(), user);
	}

	public String getToken() {
		return token;
	}

	public User getUser() {
		return user;
	}

	public boolean isValid() {
		if (this.token != null && this.user != null) {
			return true;
		}
		return false;
	}

	private static String generateSessionId() {
		// Source: http://stackoverflow.com/a/41156
		SecureRandom sr = SecureRandomSingleton.getInstance();
		return new BigInteger(SESSION_ID_LENGTH, sr).toString(32);
	}
}
