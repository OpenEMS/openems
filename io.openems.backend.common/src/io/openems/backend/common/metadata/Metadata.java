package io.openems.backend.common.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.event.EventAdmin;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonObject;

import io.openems.backend.common.event.BackendEventConstants;
import io.openems.common.OpenemsOEM;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.GetEdgesRequest.PaginationOptions;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component.Channel;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailState;

@ProviderType
public interface Metadata {

	/**
	 * Was the Metadata service fully initialized?.
	 *
	 * <p>
	 * The service might take some time in the beginning to establish a connection
	 * or to cache data from an external database.
	 *
	 * @return true if it is initialized
	 */
	public boolean isInitialized();

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
	 * Handles operations with Edge.
	 *
	 * <p>
	 * To be completed. This should eventually replace Edge.
	 *
	 * @return an {@link EdgeHandler}
	 */
	public EdgeHandler edge();

	/**
	 * Gets the Edge-ID for an API-Key, i.e. authenticates the API-Key.
	 *
	 * @param apikey the API-Key
	 * @return the Edge-ID or Empty
	 */
	public abstract Optional<String> getEdgeIdForApikey(String apikey);

	/**
	 * Get an Edge by its unique Edge-ID.
	 *
	 * @param edgeId the Edge-ID
	 * @return the Edge as Optional
	 */
	public abstract Optional<Edge> getEdge(String edgeId);

	/**
	 * Get an Edge by its unique Edge-ID. Throws an Exception if there is no Edge
	 * with this ID.
	 *
	 * @param edgeId the Edge-ID
	 * @return the Edge
	 * @throws OpenemsException on error
	 */
	public default Edge getEdgeOrError(String edgeId) throws OpenemsException {
		var edgeOpt = this.getEdge(edgeId);
		if (edgeOpt.isPresent()) {
			return edgeOpt.get();
		}
		throw new OpenemsException("Unable to get Edge for id [" + edgeId + "]");
	}

	/**
	 * Get an Edge by Edge-SetupPassword.
	 *
	 * @param setupPassword to find Edge
	 * @return Edge as a Optional
	 */
	public abstract Optional<Edge> getEdgeBySetupPassword(String setupPassword);

	/**
	 * Gets the User for the given User-ID.
	 *
	 * @param userId the User-ID
	 * @return the {@link User}, or Empty
	 */
	public abstract Optional<User> getUser(String userId);

	/**
	 * Gets all Offline-Edges.
	 *
	 * @return collection of Edges.
	 */
	public abstract Collection<Edge> getAllOfflineEdges();
	
	/**
	 * Gets all Edges that do not contain Sum State OK.
	 *
	 * @return collection of Edges.
	 */
	public abstract Collection<Edge> getAllSumNotOkEdges();

	/**
	 * Assigns Edge with given setupPassword to the logged in user and returns it.
	 *
	 * <p>
	 * If the setupPassword is invalid, an OpenemsNamedException is thrown.
	 *
	 * @param user          the {@link User}
	 * @param setupPassword the Setup-Password
	 * @return the Edge for the given Setup-Password
	 * @throws OpenemsNamedException on error
	 */
	public default Edge addEdgeToUser(User user, String setupPassword) throws OpenemsNamedException {
		var edgeOpt = this.getEdgeBySetupPassword(setupPassword);
		if (!edgeOpt.isPresent()) {
			throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
		}

		var edge = edgeOpt.get();
		this.addEdgeToUser(user, edge);

		return edge;
	}

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

	/**
	 * Helper method for creating a String of all active State-Channels by Level.
	 *
	 * @param activeStateChannels Map of ChannelAddress and
	 *                            EdgeConfig.Component.Channel; as returned by
	 *                            Edge.onSetSumState()
	 * @return a string
	 */
	public static String activeStateChannelsToString(
			Map<ChannelAddress, EdgeConfig.Component.Channel> activeStateChannels) {
		// Sort active State-Channels by Level and Component-ID
		var states = new HashMap<Level, HashMultimap<String, Channel>>();
		for (Entry<ChannelAddress, Channel> entry : activeStateChannels.entrySet()) {
			var detail = entry.getValue().getDetail();
			if (detail instanceof ChannelDetailState) {
				var level = ((ChannelDetailState) detail).getLevel();
				var channelsByComponent = states.get(level);
				if (channelsByComponent == null) {
					channelsByComponent = HashMultimap.create();
					states.put(level, channelsByComponent);
				}
				channelsByComponent.put(//
						entry.getKey().getComponentId(), //
						entry.getValue());
			}
		}
		var result = new StringBuilder();
		for (Level level : Level.values()) {
			var channelsByComponent = states.get(level);
			if (channelsByComponent != null) {
				if (result.length() > 0) {
					result.append("| ");
				}
				result.append(level.name() + ": ");
				var subResult = new StringBuilder();
				for (Entry<String, Collection<Channel>> entry : channelsByComponent.asMap().entrySet()) {
					if (subResult.length() > 0) {
						subResult.append("; ");
					}
					subResult.append(entry.getKey() + ": ");
					subResult.append(entry.getValue().stream() //
							.map(channel -> {
								if (!channel.getText().isEmpty()) {
									return channel.getText();
								}
								return channel.getId();
							}) //
							.collect(Collectors.joining(", ")));
				}
				result.append(subResult);
			}
		}
		return result.toString();
	}

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
	 * Returns the Setup Protocol PDF for the given id.
	 *
	 * @param user            {@link User} the current user
	 * @param setupProtocolId the setup protocol id to search
	 * @return the Setup Protocol PDF as a byte array
	 * @throws OpenemsNamedException on error
	 */
	public byte[] getSetupProtocol(User user, int setupProtocolId) throws OpenemsNamedException;

	/**
	 * Return the Setup Protocol data as a JsonObject.
	 *
	 * @param user   {@link User} the current user
	 * @param edgeId the {@link Edge} ID to get the data
	 * @return Setup Protocol as a JsonObject
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getSetupProtocolData(User user, String edgeId) throws OpenemsNamedException;

	/**
	 * Submit the installation assistant protocol.
	 *
	 * @param user       {@link User} the current user
	 * @param jsonObject {@link JsonObject} the setup protocol
	 * @return id of created setup protocol
	 * @throws OpenemsNamedException on error
	 */
	public int submitSetupProtocol(User user, JsonObject jsonObject) throws OpenemsNamedException;

	/**
	 * Register a user.
	 *
	 * @param user {@link JsonObject} that represents an user
	 * @param oem  OEM name
	 * @throws OpenemsNamedException on error
	 */
	public void registerUser(JsonObject user, OpenemsOEM.Manufacturer oem) throws OpenemsNamedException;

	/**
	 * Update language from given user.
	 *
	 * @param user     {@link User} the current user
	 * @param language to set language
	 * @throws OpenemsNamedException on error
	 */
	public void updateUserLanguage(User user, Language language) throws OpenemsNamedException;

	/**
	 * Gets all the alerting settings for given edge id.
	 *
	 *
	 * @param edgeId the Edge ID
	 * @return List of {@link AlertingSetting}
	 * @throws OpenemsException on error
	 */
	public List<AlertingSetting> getUserAlertingSettings(String edgeId) throws OpenemsException;

	/**
	 * Gets the alerting settings for given edge id and userId.
	 *
	 * @param edgeId the Edge ID
	 * @param userId the User ID
	 * @return List of {@link UserRoleDelayTime}
	 * @throws OpenemsException on error
	 */
	public AlertingSetting getUserAlertingSettings(String edgeId, String userId) throws OpenemsException;

	/**
	 * Sets the alerting settings for the given list of users.
	 *
	 * @param user   {@link User} the current user
	 * @param edgeId the Edge-ID
	 * @param users  list of users to update
	 * @throws OpenemsException on error
	 */
	public void setUserAlertingSettings(User user, String edgeId, List<AlertingSetting> users) throws OpenemsException;

	/**
	 * Returns an EventAdmin, used by Edge objects.
	 *
	 * @return {@link EventAdmin}
	 */
	public EventAdmin getEventAdmin();

	/**
	 * Defines Events a Metadata can throw.
	 */
	public static final class Events {
		private static final String TOPIC_BASE = BackendEventConstants.TOPIC_BASE + "metadata/";

		public static final String AFTER_IS_INITIALIZED = Events.TOPIC_BASE + "TOPIC_AFTER_IS_INITIALIZED";
	}

	/**
	 * Get serial number for the given {@link Edge}.
	 *
	 * @param edge {@link Edge} to search for serial number
	 * @return Serial number or empty {@link Optional}
	 */
	public Optional<String> getSerialNumberForEdge(Edge edge);

	/**
	 * Gets a map of Edge-IDs with the role of the given user.
	 * 
	 * @param user              {@link User} the current user
	 * @param paginationOptions the options of the requesting page
	 * @return the role to the Edge-IDs
	 * @throws OpenemsNamedException on error
	 */
	public Map<String, Role> getPageDevice(User user, PaginationOptions paginationOptions) throws OpenemsNamedException;

	/**
	 * Gets the Role for a edge of the current user.
	 * 
	 * @param user   {@link User} the current user
	 * @param edgeId the Edge-ID
	 * @return the role to the edge
	 * @throws OpenemsNamedException on error
	 */
	public Role getRoleForEdge(User user, String edgeId) throws OpenemsNamedException;

}
