package io.openems.backend.authentication.oauth2;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.DebugMode;

@ObjectClassDefinition(name = "Authentication.OAuth")
public @interface OAuthUserAuthenticationServiceConfig {

	@AttributeDefinition(name = "Realm")
	String realm() default "openems";

	@AttributeDefinition(name = "Base Keycloak URL", description = "The base URL of the Keycloak server. This is used to construct the URLs for the OAuth2 endpoints.")
	String baseKeycloakUrl() default "http://localhost:7900";

	@AttributeDefinition(name = "Issuer URL", description = "The Issuer URL of the OAuth2 server. This is used to validate the tokens issued by the OAuth2 server.", defaultValue = "http://localhost:7900/realms/openems")
	String issuerUrl() default "http://localhost:7900/realms/openems";

	@AttributeDefinition(name = "Login URL", description = "The URL to redirect the user to for login. This is the OAuth2 authorization endpoint.", defaultValue = "http://localhost:7900/realms/openems/protocol/openid-connect/auth")
	String loginUrl() default "http://localhost:7900/realms/openems/protocol/openid-connect/auth";

	@AttributeDefinition(name = "Token URL", description = "The URL to exchange the authorization code for an access token. This is the OAuth2 token endpoint.", defaultValue = "http://localhost:7900/realms/openems/protocol/openid-connect/token")
	String tokenUrl() default "http://localhost:7900/realms/openems/protocol/openid-connect/token";

	@AttributeDefinition(name = "Certificate URL", description = "The URL to retrieve the public keys used to verify the JWT tokens issued by the OAuth2 server. This is the OAuth2 JWKs endpoint.", defaultValue = "http://localhost:7900/realms/openems/protocol/openid-connect/certs")
	String certsUrl() default "http://localhost:7900/realms/openems/protocol/openid-connect/certs";

	@AttributeDefinition(name = "Rate Limiter Bucket size")
	int rateLimitedBucketSize() default 10;

	@AttributeDefinition(name = "Rate Limiter refill rate per second")
	int rateLimitedRefillRate() default 1;

	@AttributeDefinition(name = "Max number of concurrent connections", description = "Maximum number of concurrent request to the OAuth2 server")
	int maxConcurrentRequests() default 50;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	DebugMode debugMode() default DebugMode.OFF;

}
