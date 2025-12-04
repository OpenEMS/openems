package io.openems.edge.io.phoenixcontact.auth;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(scope = ServiceScope.SINGLETON, service = PlcNextTokenManager.class)
public class PlcNextTokenManager {

	public static final long TOKEN_VALIDITY_PERIOD_IN_MINUTES = 5L;

	private static final Logger log = LoggerFactory.getLogger(PlcNextTokenManager.class);

	private final PlcNextAuthClient authClient;

	private String token;
	private ZonedDateTime tokenExpirery;

	@Activate
	public PlcNextTokenManager(@Reference(scope = ReferenceScope.BUNDLE) PlcNextAuthClient authClient) {
		this.authClient = authClient;
	}

	/**
	 * Initialize fetching valid JWT periodically
	 */
	public synchronized void fetchToken(PlcNextAuthClientConfig authClientConfig) {
		if (isTokenRequestAllowed()) {
			log.info("Start fetching authentication");
			this.token = this.authClient.fetchSingleAuthentication(authClientConfig);
			this.tokenExpirery = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS)
					.plusMinutes(TOKEN_VALIDITY_PERIOD_IN_MINUTES);
			log.info("Fetching authentication finished. Got access token? " + Objects.nonNull(this.token));
		} else {
			log.info("Token still valid, skipping token refresh.");
		}
	}

	private boolean isTokenRequestAllowed() {
		return Objects.isNull(tokenExpirery) || tokenExpirery.isBefore(ZonedDateTime.now());
	}


	public synchronized String getToken() {
		return this.token;
	}
}
