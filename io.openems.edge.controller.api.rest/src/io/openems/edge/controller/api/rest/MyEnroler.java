package io.openems.edge.controller.api.rest;

import org.restlet.Application;
import org.restlet.data.ClientInfo;
import org.restlet.security.Enroler;
import org.restlet.security.Role;

public class MyEnroler implements Enroler {

	@Override
	public void enrole(ClientInfo clientInfo) {
		// TODO
//		String username = clientInfo.getUser().getIdentifier();
//		User user;
//		try {
//			user = User.getUserByName(username);
			clientInfo.getRoles().add( //
//					Role.get(Application.getCurrent(), user.getRole().name().toLowerCase()) //
					Role.get(Application.getCurrent(), "admin") //
			);
//		} catch (OpenemsException e) { /* ignore, just don't enrole user in any group */ }
	}
}
