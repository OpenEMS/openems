package io.openems.impl.controller.api.rest.internal;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.security.Verifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.security.Authentication;
import io.openems.api.security.Session;

public class OpenemsVerifier implements Verifier {

	private final static Logger log = LoggerFactory.getLogger(OpenemsVerifier.class);

	@Override public int verify(Request request, Response response) {
		if (request.getChallengeResponse() == null) {
			log.warn("Authentication failed: No authentication data available.");
			return RESULT_MISSING;
		} else {
			String username = getIdentifier(request, response);
			String password = new String(getSecret(request, response));
			Session session = Authentication.getInstance().byUserPassword(username, password);

			if (session == null || !session.isValid()) {
				log.warn("Authentication failed.");
				return RESULT_INVALID;
			} else {
				// log.info("Authentication successful: logged in as " + user.getName());
				request.getClientInfo().setUser(new org.restlet.security.User(session.getUser().getName()));
				request.getChallengeResponse().setIdentifier(session.getUser().getName());
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
