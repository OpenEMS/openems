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

public abstract class SessionManager<T extends Session<?>, V extends SessionData> {

	private final static int SESSION_ID_LENGTH = 130;

	// TODO: invalidate old sessions in separate thread: call _removeSession to do so
	private final Map<String, T> sessions = new ConcurrentHashMap<>();
	
	protected SessionManager() {}
	
	public T createNewSession(String token, V data) {
		T session = this._createNewSession(token, data);
		this._putSession(token, session);
		return session;
	}
	
	public T createNewSession(V data) {
		String token = this.generateToken();
		return this.createNewSession(token, data);
	}
	
	public Optional<T> getSessionByToken(String token) {
		return Optional.ofNullable(this.sessions.get(token));
	}
	
	public void removeSession(String token) {
		T session = this.sessions.get(token);
		if(session != null) {
			session.setInvalid();
			this._removeSession(token);
		}
	}
	
	public void removeSession(Session<?> session) {
		session.setInvalid();
		this.removeSession(session.getToken());
	}
	
	protected String generateToken() {
		// Source: http://stackoverflow.com/a/41156
		SecureRandom sr = SecureRandomSingleton.getInstance();
		return new BigInteger(SESSION_ID_LENGTH, sr).toString(32);
	}
	
	/*
	 * Those methods are prone to be overwritten by inheritance
	 */
	/**
	 * Replies a Session object of type T
	 * 
	 * @param token
	 * @param data
	 * @return
	 */
	protected abstract T _createNewSession(String token, V data);
	
	/**
	 * This method is always called when adding a session to local database 
	 * 
	 * @param token
	 * @param session
	 */
	protected void _putSession(String token, T session) {
		this.sessions.put(token, session);
	}
	
	/**
	 * This method is always called when removing a session from local database 
	 * 
	 * @param token
	 * @param session
	 */
	protected void _removeSession(String token) {
		this.sessions.remove(token);
	}
}
