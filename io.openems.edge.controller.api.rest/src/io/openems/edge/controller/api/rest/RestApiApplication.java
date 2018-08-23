package io.openems.edge.controller.api.rest;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.data.ChallengeScheme;
import org.restlet.routing.Router;
import org.restlet.security.ChallengeAuthenticator;

import io.openems.edge.controller.api.rest.route.ChannelRestlet;
import io.openems.edge.controller.api.rest.route.UserChangePasswordRestlet;

public class RestApiApplication extends Application {

	private final RestApi parent;

	public RestApiApplication(RestApi parent) {
		this.parent = parent;
	}

	/**
	 * Creates a root Restlet that will receive all incoming calls.
	 */
	@Override
	public synchronized Restlet createInboundRoot() {
		ChallengeAuthenticator guard = createAuthenticator();
		Router router = createRouter();
		guard.setNext(router);
		return guard;
	}

	private ChallengeAuthenticator createAuthenticator() {
		ChallengeAuthenticator guard = new ChallengeAuthenticator(getContext(), ChallengeScheme.HTTP_BASIC,
				"OpenEMS REST-Api");
		guard.setVerifier(new MyVerifier(this.parent));
		return guard;
	}

	private Router createRouter() {
		Router router = new Router(getContext());
		router.attach("/user/changePassword", new UserChangePasswordRestlet(this.parent));
		router.attach("/channel/{thing}/{channel}", new ChannelRestlet(this.parent));
		return router;
	}
}
