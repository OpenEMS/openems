package io.openems.edge.controller.api.rest;

import java.util.Optional;

import org.restlet.Application;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Role;
import org.restlet.security.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.user.User;

public class MyVerifier implements Verifier {

	private final Logger log = LoggerFactory.getLogger(MyVerifier.class);
	private final RestApi parent;

	public MyVerifier(RestApi parent) {
		this.parent = parent;
	}

	@Override
	public int verify(Request request, Response response) {
		if (request.getChallengeResponse() == null) {
			this.parent.logWarn(this.log, "Authentication failed: No authentication data available.");
			return RESULT_MISSING;
		} else {
			String username = getIdentifier(request, response);
			String password = new String(getSecret(request, response));
			Optional<User> userOpt = parent.userService.authenticate(username, password);
			if (userOpt.isPresent()) {
				User user = userOpt.get();
				request.getClientInfo().setUser(new org.restlet.security.User(user.getName()));
				request.getClientInfo().getRoles().add( //
						Role.get(Application.getCurrent(), user.getRole().name().toLowerCase()));
				request.getChallengeResponse().setIdentifier(user.getName());
				return RESULT_VALID;
			} else {
				this.parent.logWarn(this.log, "Authentication failed.");
				return RESULT_INVALID;
			}
		}
	}

	/**
	 * Returns the user identifier.
	 *
	 * @param request  The request to inspect.
	 * @param response The response to inspect.
	 * @return The user identifier.
	 */
	protected String getIdentifier(Request request, Response response) {
		return request.getChallengeResponse().getIdentifier();
	}

	/**
	 * Returns the secret provided by the user.
	 *
	 * @param request  The request to inspect.
	 * @param response The response to inspect.
	 * @return The secret provided by the user.
	 */
	protected char[] getSecret(Request request, Response response) {
		return request.getChallengeResponse().getSecret();
	}

}
