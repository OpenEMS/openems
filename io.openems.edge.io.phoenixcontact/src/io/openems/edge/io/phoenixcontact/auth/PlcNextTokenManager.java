package io.openems.edge.io.phoenixcontact.auth;

import java.util.Objects;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.io.phoenixcontact.PlcNextAuthClient;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextTokenManager.class)
public class PlcNextTokenManager {

	private static final Logger log = LoggerFactory.getLogger(PlcNextTokenManager.class);

	private final PlcNextAuthClient authClient;

	private String token;

	@Activate
	public PlcNextTokenManager(@Reference(scope = ReferenceScope.BUNDLE) PlcNextAuthClient authClient) {
		this.authClient = authClient;
		
		fetchToken();
	}

	/**
	 * Initialize fetching valid JWT periodically
	 */
	public void fetchToken() {
		log.info("Start fetching authentication");
		this.token = this.authClient.fetchSingleAuthentication();
		log.info("Fetching authentication finished. Got access token? " + Objects.nonNull(this.token));
	}

	public synchronized String getToken() {
		return this.token;
	}
}
