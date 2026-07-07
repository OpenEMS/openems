package io.openems.backend.authentication.oauth2;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Authentication.OAuth.Client")
public @interface OAuthOemConfig {

	@AttributeDefinition(name = "OEM")
	String oem() default "openems";

	@AttributeDefinition(//
			name = "Client ID", //
			description = "The Client ID for the OAuth2 Client. This is used to identify the client to the OAuth2 server.")
	String clientId() default "openems";

	@AttributeDefinition(//
			name = "Client Secret", //
			description = "The Client Secret for the OAuth2 Client. This is used to authenticate the client with the OAuth2 server.", //
			type = AttributeType.PASSWORD)
	String clientSecret();

	@AttributeDefinition(//
			name = "Default redirect URI", //
			description = "The redirect URI to use. This is used when the user is redirected back to the application after authentication.")
	String redirectUri() default "http://localhost:4200/";

	@AttributeDefinition(name = "Is service account?", description = "The service account to create new accounts. "//
			+ "'Service accounts roles' has to be enabled in keycloak for this client and roles of the service account need to be properly set.")
	boolean serviceAccount() default false;

}
