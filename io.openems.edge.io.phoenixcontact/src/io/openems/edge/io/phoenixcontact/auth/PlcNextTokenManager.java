package io.openems.edge.io.phoenixcontact.auth;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import com.google.gson.JsonObject;

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
	private void fetchToken() {
		this.authClient.fetchAuthenticationPeriodically((a, b) -> {
			if (a == null) {
				return;
			}
			JsonObject jsonObject = a.data().getAsJsonObject();
			
			// TODO check if the token is expires
			this.token = jsonObject.getAsJsonPrimitive("jwtToken").getAsString();
		});
	}

	public synchronized String getToken() {
		if (this.token == null) {
			this.token = authClient.fetchSingleAuthentication();
		}
		return this.token;
	}
}
