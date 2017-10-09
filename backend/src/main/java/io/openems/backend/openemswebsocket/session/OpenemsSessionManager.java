package io.openems.backend.openemswebsocket.session;

import java.util.Map.Entry;
import java.util.Optional;

import com.google.common.collect.HashMultimap;

import io.openems.common.session.SessionManager;

public class OpenemsSessionManager extends SessionManager<OpenemsSession, OpenemsSessionData> {

	// mapping between Token (apikey) and device name
	private final HashMultimap<String, String> token2names = HashMultimap.create();

	@Override
	public OpenemsSession _createNewSession(String token, OpenemsSessionData data) {
		return new OpenemsSession(token, data);
	}

	@Override
	protected void _putSession(String token, OpenemsSession session) {
		super._putSession(token, session);
		synchronized (this.token2names) {
			for (String deviceName : session.getData().getDevices().getNames()) {
				this.token2names.put(token, deviceName);
			}
		}
	}

	@Override
	protected void _removeSession(String token) {
		super._removeSession(token);
		synchronized (this.token2names) {
			this.token2names.removeAll(token);
		}
	}

	public Optional<OpenemsSession> getSessionByDeviceName(String name) {
		String token = null;
		synchronized (this.token2names) {
			if (this.token2names.containsValue(name)) {
				for (Entry<String, String> entry : this.token2names.entries()) {
					if (entry.getValue().equals(name)) {
						token = entry.getKey();
					}
				}
			}
		}
		if (token == null) {
			return Optional.empty();
		}
		return super.getSessionByToken(token);
	}
}
