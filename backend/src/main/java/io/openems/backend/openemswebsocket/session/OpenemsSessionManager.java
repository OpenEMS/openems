package io.openems.backend.openemswebsocket.session;

import java.util.Optional;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

import io.openems.common.session.SessionManager;

public class OpenemsSessionManager extends SessionManager<OpenemsSession, OpenemsSessionData> {

	// mapping between Token (apikey) and device name
	private final BiMap<String, String> token2name = Maps.synchronizedBiMap(HashBiMap.create());

	@Override
	public OpenemsSession _createNewSession(String token, OpenemsSessionData data) {
		return new OpenemsSession(token, data);
	}

	@Override
	protected void _putSession(String token, OpenemsSession session) {
		super._putSession(token, session);
		this.token2name.put(token, session.getData().getDevice().getName());
	}

	@Override
	protected void _removeSession(String token) {
		super._removeSession(token);
		this.token2name.remove(token);
	}

	public Optional<OpenemsSession> getSessionByDeviceName(String name) {
		String token = this.token2name.inverse().get(name);
		if (token == null) {
			return Optional.empty();
		}
		return super.getSessionByToken(token);
	}
}
