package io.openems.backend.authentication.api;

import java.util.concurrent.CompletableFuture;

import io.openems.backend.authentication.api.model.PasswordAuthenticationResult;

public interface AuthUserPasswordAuthenticationService {

	/**
	 * Authenticates a user with the given username and password.
	 * 
	 * @param username the username of the user
	 * @param password the password of the user
	 * @return a {@link CompletableFuture} that completes with the authentication
	 *         result
	 */
	CompletableFuture<PasswordAuthenticationResult> authenticateWithPassword(String username, String password);

	/**
	 * Authenticates a user with the given token.
	 * 
	 * @param token the token of the user
	 * @return a {@link CompletableFuture} that completes with the authentication
	 */
	CompletableFuture<PasswordAuthenticationResult> authenticateWithToken(String token);

	/**
	 * Logs out a user with the given token.
	 * 
	 * @param token the token of the user
	 * @return a {@link CompletableFuture} that completes when the logout is done
	 */
	CompletableFuture<Void> logout(String token);

}
