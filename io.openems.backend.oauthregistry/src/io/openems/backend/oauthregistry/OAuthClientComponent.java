package io.openems.backend.oauthregistry;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = OAuthClientConfiguration.class, factory = true)
@Component(//
		name = "OAuthRegistry.Client", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class OAuthClientComponent implements OAuthClient {

	private final OAuthClientConfiguration configuration;

	@Activate
	public OAuthClientComponent(OAuthClientConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public String identifier() {
		return this.configuration.identifier();
	}

	@Override
	public String authenticationUrl() {
		return this.configuration.authenticationUrl();
	}

	@Override
	public String codeToTokenUrl() {
		return this.configuration.tokenUrl();
	}

	@Override
	public String clientId() {
		return this.configuration.clientId();
	}

	@Override
	public String clientSecret() {
		return this.configuration.clientSecret();
	}

	@Override
	public String issuer() {
		return this.configuration.issuerUrl();
	}

	@Override
	public String redirectUri() {
		return this.configuration.redirectUri();
	}
}