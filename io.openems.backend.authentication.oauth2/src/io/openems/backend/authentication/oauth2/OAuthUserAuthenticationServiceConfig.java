package io.openems.backend.authentication.oauth2;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.DebugMode;

@ObjectClassDefinition(name = "Authentication.OAuth")
public @interface OAuthUserAuthenticationServiceConfig {

	@AttributeDefinition(name = "Realm", description = "The realm name (used for Keycloak Admin API)")
	String realm() default "openems";

	@AttributeDefinition(name = "Issuer URL", description = "The Issuer URL of the OIDC provider. "
			+ "All other endpoints (authorization, token, logout, jwks) are automatically discovered via "
			+ "/.well-known/openid-configuration")
	String issuerUrl() default "http://localhost:7900/realms/openems";

	@AttributeDefinition(name = "Rate Limiter Bucket size")
	int rateLimitedBucketSize() default 10;

	@AttributeDefinition(name = "Rate Limiter refill rate per second")
	int rateLimitedRefillRate() default 1;

	@AttributeDefinition(name = "Max number of concurrent connections", description = "Maximum number of concurrent request to the OAuth2 server")
	int maxConcurrentRequests() default 50;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	DebugMode debugMode() default DebugMode.OFF;

}
