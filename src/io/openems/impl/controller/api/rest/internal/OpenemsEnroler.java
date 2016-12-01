package io.openems.impl.controller.api.rest.internal;

import org.restlet.Application;
import org.restlet.data.ClientInfo;
import org.restlet.security.Enroler;
import org.restlet.security.Role;

import io.openems.api.exception.OpenemsException;
import io.openems.api.security.User;

public class OpenemsEnroler implements Enroler {

	@Override public void enrole(ClientInfo clientInfo) {
		String username = clientInfo.getUser().getIdentifier();
		User user;
		try {
			user = User.getUserByName(username);
			// Add all roles for this user
			for (User role : user.getRoles()) {
				Role restletRole = Role.get(Application.getCurrent(), role.getName());
				clientInfo.getRoles().add(restletRole);
			}
		} catch (OpenemsException e) { /* ignore, just don't enrole user in any group */ }
	}
}
