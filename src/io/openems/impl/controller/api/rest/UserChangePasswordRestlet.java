package io.openems.impl.controller.api.rest;

import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.data.Status;
import org.restlet.resource.ResourceException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.openems.api.exception.OpenemsException;
import io.openems.api.exception.ReflectionException;
import io.openems.api.security.User;
import io.openems.core.utilities.JsonUtils;

public class UserChangePasswordRestlet extends OpenemsRestlet {

	public UserChangePasswordRestlet() {
		super();
	}

	@Override public void handle(Request request, Response response) {
		super.handle(request, response);

		// get user
		User user;
		try {
			user = User.getUserByName(request.getClientInfo().getUser().getIdentifier());
		} catch (OpenemsException e) {
			// User not found
			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
		}

		// check permission
		if (!isAuthenticatedAsUser(request, user)) {
			throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
		}

		// call handler methods
		if (request.getMethod().equals(Method.POST)) {
			JsonParser parser = new JsonParser();
			String httpPost = request.getEntityAsText();
			JsonObject jHttpPost = parser.parse(httpPost).getAsJsonObject();
			changePassword(user, jHttpPost);
		}
	}

	/**
	 * handle HTTP POST request
	 *
	 * @param thingId
	 * @param channelId
	 * @param jHttpPost
	 */
	private void changePassword(User user, JsonObject jHttpPost) {
		// parse old and new password
		String oldPassword;
		String newPassword;
		try {
			oldPassword = JsonUtils.getAsString(jHttpPost, "oldPassword");
			newPassword = JsonUtils.getAsString(jHttpPost, "newPassword");
		} catch (ReflectionException e1) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Value is missing");
		}

		try {
			user.changePassword(oldPassword, newPassword);
			log.info("Changed password for user [" + user.getName() + "].");
		} catch (OpenemsException e) {
			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Changing password failed: " + e.getMessage());
		}
	}
}
