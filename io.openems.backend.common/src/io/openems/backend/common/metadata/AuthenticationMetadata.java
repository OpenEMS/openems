package io.openems.backend.common.metadata;

import java.util.Map;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;

/**
 * This implementation of EdgeBackendMetadata reads Edges configuration from a file.
 * The layout of the file is as follows:
 *
 * <pre>
 * {
 *   edges: {
 *     [edgeId: string]: {
 *       comment: string,
 *       apikey: string
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>
 * Combine this service with an AuthenticationMetdata to retrieve Users and Permissions.
 */
@ProviderType
public interface AuthenticationMetadata {
	
	/**
	 * Authenticates the User by username and password.
	 *
	 * @param username the Username
	 * @param password the Password
	 * @return the {@link User}
	 * @throws OpenemsNamedException on error
	 */
	public User authenticate(String username, String password) throws OpenemsNamedException;

	/**
	 * Authenticates the User by a Token.
	 *
	 * @param token the Token
	 * @return the {@link User}
	 * @throws OpenemsNamedException on error
	 */
	public User authenticate(String token) throws OpenemsNamedException;

	/**
	 * Closes a session for a User.
	 *
	 * @param user the {@link User}
	 */
	public void logout(User user);
	
	/**
	 * Gets the User for the given User-ID.
	 *
	 * @param userId the User-ID
	 * @return the {@link User}, or Empty
	 */
	public abstract Optional<User> getUser(String userId);
	
	/**
	 * Update language from given user.
	 *
	 * @param user     {@link User} the current user
	 * @param language to set language
	 * @throws OpenemsNamedException on error
	 */
	public void updateUserLanguage(User user, Language language) throws OpenemsNamedException;
	
	/**
	 * Register a user.
	 *
	 * @param jsonObject {@link JsonObject} that represents an user
	 * @throws OpenemsNamedException on error
	 */
	public void registerUser(JsonObject jsonObject) throws OpenemsNamedException;
	
	/**
	 * Gets information about the given user {@link User}.
	 *
	 * @param user {@link User} to read information
	 * @return {@link Map} about the user
	 * @throws OpenemsNamedException on error
	 */
	public Map<String, Object> getUserInformation(User user) throws OpenemsNamedException;

	/**
	 * Update the given user {@link User} with new information {@link JsonObject}.
	 *
	 * @param user       {@link User} to update
	 * @param jsonObject {@link JsonObject} information about the user
	 * @throws OpenemsNamedException on error
	 */
	public void setUserInformation(User user, JsonObject jsonObject) throws OpenemsNamedException;
	
	/**
	 * Assigns Edge to current user.
	 *
	 * <p>
	 * If assignment fails, an OpenemsNamedException is thrown.
	 *
	 * @param user The {@link User}
	 * @param edge The {@link Edge}
	 *
	 * @throws OpenemsNamedException on error
	 */
	public void addEdgeToUser(User user, Edge edge) throws OpenemsNamedException;
}
