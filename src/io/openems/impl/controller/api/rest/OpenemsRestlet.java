package io.openems.impl.controller.api.rest;

import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.security.User;

public abstract class OpenemsRestlet extends Restlet {
	protected final Logger log;

	public OpenemsRestlet() {
		this.log = LoggerFactory.getLogger(this.getClass());
	}

	protected boolean isAuthenticatedAsUser(Request request, User user) {
		if (user.equals(User.ADMIN)) {
			// ADMIN is allowed to do anything
			return true;
		}
		return request.getClientInfo().getRoles().contains(Role.get(Application.getCurrent(), user.getName()));
	}

	@Override public void handle(Request request, Response response) {
		super.handle(request, response);
	}
}
