package io.openems.edge.controller.api.rest.route;

import org.restlet.Request;
import org.restlet.Response;

import io.openems.edge.controller.api.rest.MyRestlet;
import io.openems.edge.controller.api.rest.RestApi;

public class UserChangePasswordRestlet extends MyRestlet {

//	private final Logger log = LoggerFactory.getLogger(UserChangePasswordRestlet.class);
//	private final RestApi parent;

	public UserChangePasswordRestlet(RestApi parent) {
		super();
//		this.parent = parent;
	}

	@Override
	public void handle(Request request, Response response) {
		super.handle(request, response);
		// TODO
//		// get user
//		User user;
//		try {
//			user = User.getUserByName(request.getClientInfo().getUser().getIdentifier());
//		} catch (OpenemsException e) {
//			// User not found
//			throw new ResourceException(Status.CLIENT_ERROR_NOT_FOUND);
//		}
//
//		// check permission
//		if (!isAuthenticatedAsRole(request, user.getRole())) {
//			throw new ResourceException(Status.CLIENT_ERROR_UNAUTHORIZED);
//		}
//
//		// call handler methods
//		if (request.getMethod().equals(Method.POST)) {
//			JsonParser parser = new JsonParser();
//			String httpPost = request.getEntityAsText();
//			JsonObject jHttpPost = parser.parse(httpPost).getAsJsonObject();
//			changePassword(user, jHttpPost);
//		}
	}

	/**
	 * handle HTTP POST request
	 *
	 * @param thingId
	 * @param channelId
	 * @param jHttpPost
	 */
//	private void changePassword(User user, JsonObject jHttpPost) {
//		// parse old and new password
//		String oldPassword;
//		String newPassword;
//		try {
//			oldPassword = JsonUtils.getAsString(jHttpPost, "oldPassword");
//			newPassword = JsonUtils.getAsString(jHttpPost, "newPassword");
//		} catch (OpenemsException e1) {
//			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Value is missing");
//		}
//
//		try {
//			user.changePassword(oldPassword, newPassword);
//			log.info("Changed password for user [" + user.getName() + "].");
//		} catch (OpenemsException e) {
//			throw new ResourceException(Status.CLIENT_ERROR_BAD_REQUEST, "Changing password failed: " + e.getMessage());
//		}
//	}
}
