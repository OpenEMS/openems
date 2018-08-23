package io.openems.edge.controller.api.rest;

import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;

import io.openems.common.session.Role;

public abstract class MyRestlet extends Restlet {

	protected boolean isAuthenticatedAsRole(Request request, Role role) {
		return request.getClientInfo().getRoles().contains( //
				org.restlet.security.Role.get( //
						Application.getCurrent(), role.name().toLowerCase()));
	}

	@Override
	public void handle(Request request, Response response) {
		super.handle(request, response);
	}

}
