package io.openems.backend.oauthregistry;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "OAuthRegistry.ClientConfiguration")
public @interface OAuthClientConfiguration {

	@AttributeDefinition(name = "Identifier", description = "Unique identifier for this OAuth client configuration, used for internal mapping")
	String identifier();

	@AttributeDefinition(name = "Authentication URL", description = "The Authentication-URL the user is redirected to.")
	String authenticationUrl();

	@AttributeDefinition(name = "Token URL", description = "The Token-URL used to exchange the authorization code or refresh accessToken for an access accessToken.")
	String tokenUrl();

	@AttributeDefinition(name = "Issuer URL", description = "The Issuer-URL of the OAuth provider.")
	String issuerUrl();

	@AttributeDefinition(name = "Client Id")
	String clientId();

	@AttributeDefinition(name = "Client Secret", type = AttributeType.PASSWORD)
	String clientSecret();

	@AttributeDefinition(name = "Redirect URI", description = "The Redirect URI registered with the OAuth provider.")
	String redirectUri();
}
