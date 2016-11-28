package io.openems.impl.controller.api.rest;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.security.User;

public class OpenemsVerifier implements Verifier {

	private final static Logger log = LoggerFactory.getLogger(OpenemsVerifier.class);

	@Override public int verify(Request request, Response response) {
		if (request.getChallengeResponse() == null) {
			log.warn("Authentication failed: No authentication data available.");
			return RESULT_MISSING;
		} else {
			String username = getIdentifier(request, response);
			String password = new String(getSecret(request, response));
			User user = User.authenticate(username, password);

			if (user == null) {
				log.warn("Authentication failed: wrong password.");
				return RESULT_INVALID;
			} else {
				// log.info("Authentication successful: logged in as " + user.getName());
				request.getClientInfo().setUser(new org.restlet.security.User(user.getName()));
				request.getChallengeResponse().setIdentifier(user.getName());
				return RESULT_VALID;
			}
		}
	}

	/**
	 * Returns the user identifier.
	 *
	 * @param request
	 *            The request to inspect.
	 * @param response
	 *            The response to inspect.
	 * @return The user identifier.
	 */
	protected String getIdentifier(Request request, Response response) {
		return request.getChallengeResponse().getIdentifier();
	}

	/**
	 * Returns the secret provided by the user.
	 *
	 * @param request
	 *            The request to inspect.
	 * @param response
	 *            The response to inspect.
	 * @return The secret provided by the user.
	 */
	protected char[] getSecret(Request request, Response response) {
		return request.getChallengeResponse().getSecret();
	}

}
