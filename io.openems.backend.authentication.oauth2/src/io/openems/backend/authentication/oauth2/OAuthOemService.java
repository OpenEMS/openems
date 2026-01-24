package io.openems.backend.authentication.oauth2;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = OAuthOemConfig.class, factory = true)
@Component(//
		name = "Authentication.OAuth.ClientConfig", //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		service = OAuthOemService.class, //
		immediate = true //
)
public class OAuthOemService {

	private final OAuthOemConfig config;

	@Activate
	public OAuthOemService(OAuthOemConfig config) {
		this.config = config;
	}

	public OAuthOemConfig getConfig() {
		return this.config;
	}

}
