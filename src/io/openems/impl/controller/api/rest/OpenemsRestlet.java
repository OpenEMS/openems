package io.openems.impl.controller.api.rest;

import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;
import org.restlet.security.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.security.OpenemsRole;

public abstract class OpenemsRestlet extends Restlet {
	protected final Logger log;

	public OpenemsRestlet() {
		this.log = LoggerFactory.getLogger(this.getClass());
	}

	protected boolean isInRole(Request request, io.openems.api.security.OpenemsRole role) {
		return request.getClientInfo().getRoles().contains(Role.get(Application.getCurrent(), role.toString()));
	}

	@Override public void handle(Request request, Response response) {
		super.handle(request, response);

		// check permission
		if (isInRole(request, OpenemsRole.USER) && request.getClientInfo().getRoles().size() == 1) {
			// user is only the group "USER" - deny anything but GET requests
			if (!request.getMethod().equals(Method.GET)) {
				throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
			}
		}
	}
}
