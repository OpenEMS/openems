package io.openems.backend.oauthregistry;

public interface OAuthClient {

	/**
	 * The internal oauth identifier.
	 * 
	 * @return the identifier
	 */
	String identifier();

	/**
	 * The Authentication-URL or null if dynamic via issuer.
	 * 
	 * @return the authentication url
	 */
	String authenticationUrl();

	/**
	 * The CodeToToken-URL or null if dynamic via issuer.
	 * 
	 * @return the code to accessToken url
	 */
	String codeToTokenUrl();

	/**
	 * The client id.
	 * 
	 * @return the id
	 */
	String clientId();

	/**
	 * The client secret.
	 * 
	 * @return the secret
	 */
	String clientSecret();

	/**
	 * The client issuer.
	 * 
	 * @return the issuer
	 */
	String issuer();

	/**
	 * The redirect uri.
	 * 
	 * @return the redirect uri
	 */
	String redirectUri();

}
