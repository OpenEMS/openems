package io.openems.backend.authentication.api;

import java.util.concurrent.CompletableFuture;

import io.openems.backend.authentication.api.model.request.RegisterUserRequest;

public interface AuthUserRegistrationService {

	/**
	 * Registers a new user with the given username and email.
	 *
	 * @param user the user data to register
	 * @return a {@link CompletableFuture} that completes when the registration is
	 */
	CompletableFuture<Void> registerUser(RegisterUserRequest user);

	/**
	 * Registers a new user with the given username and email.
	 *
	 * @param user the user data to register
	 * @return a {@link CompletableFuture} that completes when the registration is
	 */
	CompletableFuture<Void> registerUserIfNotExist(RegisterUserRequest user);

}
