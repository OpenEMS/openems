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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;

import io.openems.common.utils.SecureRandomSingleton;

public abstract class SessionManager<S extends Session<D>, D extends SessionData> {

	private final static int SESSION_ID_LENGTH = 130;

	// TODO: invalidate old sessions in separate thread: call _removeSession to do so
	private final Map<String, S> sessions = new ConcurrentHashMap<>();
	
	protected SessionManager() {}
	
	public S createNewSession(String token, D data) {
		S session = this._createNewSession(token, data);
		this._putSession(token, session);
		return session;
	}
	
	public S createNewSession(D data) {
		String token = this.generateToken();
		return this.createNewSession(token, data);
	}
	
	public Optional<S> getSessionByToken(String token) {
		return Optional.ofNullable(this.sessions.get(token));
	}
	
	public void removeSession(String token) {
		S session = this.sessions.get(token);
		if(session != null) {
			session.setInvalid();
			this._removeSession(token);
		}
	}
	
	public void removeSession(Session<D> session) {
		session.setInvalid();
		this.removeSession(session.getToken());
	}
	
	protected String generateToken() {
		// Source: http://stackoverflow.com/a/41156
		SecureRandom sr = SecureRandomSingleton.getInstance();
		return new BigInteger(SESSION_ID_LENGTH, sr).toString(32);
	}
	
	public Collection<S> getSessions() {
		return Collections.unmodifiableCollection(this.sessions.values());
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
	protected abstract S _createNewSession(String token, D data);
	
	/**
	 * This method is always called when adding a session to local database 
	 * 
	 * @param token
	 * @param session
	 */
	protected void _putSession(String token, S session) {
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
