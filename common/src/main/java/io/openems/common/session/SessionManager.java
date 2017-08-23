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
package io.openems.common.session;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.openems.common.utils.SecureRandomSingleton;

public abstract class SessionManager<T extends Session, V extends SessionData> {

	private final static int SESSION_ID_LENGTH = 130;

	// TODO: invalidate old sessions in separate thread
	private final Map<String, T> sessions = new ConcurrentHashMap<>();
	
	protected SessionManager() {}
	
	public T createNewSession(V data) {
		String token = this.generateToken();
		T session = this._createNewSession(token, data);
		this.sessions.put(token, session);
		return session;
	}
	
	protected abstract T _createNewSession(String token, V data);
	
	public Optional<T> getSessionByToken(String token) {
		return Optional.ofNullable(this.sessions.get(token));
	}
	
	public void removeSession(String token) {
		T session = this.sessions.get(token);
		if(session != null) {
			session.setInvalid();
			this.sessions.remove(token);
		}
	}
	
	protected String generateToken() {
		// Source: http://stackoverflow.com/a/41156
		SecureRandom sr = SecureRandomSingleton.getInstance();
		return new BigInteger(SESSION_ID_LENGTH, sr).toString(32);
	}
}
