package io.openems.edge.io.phoenixcontact.auth;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.edge.io.phoenixcontact.PlcNextAuthClient;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextTokenManager.class)
public class PlcNextTokenManager {

	private final PlcNextAuthClient authClient;

	private String token;

	@Activate
	public PlcNextTokenManager(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) PlcNextAuthClient authClient) {
		this.authClient = authClient;
		
		fetchToken();
	}

	/**
	 * Initialize fetching valid JWT periodically
	 */
	public void fetchToken() {
		this.token = this.authClient.fetchSingleAuthentication();
	}

	public synchronized String getToken() {
		return this.token;
	}
}
