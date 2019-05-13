package io.openems.backend.metadata.api;

import java.util.Collection;
import java.util.Optional;

import io.openems.common.access_control.RoleId;
import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;

@ProviderType
public interface Metadata {

    /**
     * Authenticates a User without any information.
     *
     * <p>
     * This is only useful for Dummy-Implementations. By default authentication is
     * denied in this case.
     *
     * @return the User
     * @throws OpenemsNamedException on error
     */
    default BackendUser authenticate() throws OpenemsNamedException {
        throw OpenemsError.COMMON_AUTHENTICATION_FAILED.exception();
    }

    /**
     * Authenticates the User by username and password.
     *
     * @param username the Username
     * @param password the Password
     * @return the User
     * @throws OpenemsNamedException on error
     */
    BackendUser authenticate(String username, String password) throws OpenemsNamedException;

    RoleId authenticate2(String userName, String password, String roleId) throws OpenemsException;

    /**
     * Authenticates the User by a Session-ID.
     *
     * @param sessionId the Session-ID
     * @return the User
     * @throws OpenemsNamedException on error
     */
    BackendUser authenticate(String sessionId) throws OpenemsNamedException;

    /**
     * Gets the Edge-ID for an API-Key, i.e. authenticates the API-Key.
     *
     * @param apikey the API-Key
     * @return the Edge-ID or Empty
     */
    Optional<String> getEdgeIdForApikey(String apikey);

    /**
     * Get an Edge by its unique Edge-ID.
     *
     * @param edgeId the Edge-ID
     * @return the Edge as Optional
     */
    Optional<Edge> getEdge(String edgeId);

    /**
     * Get an Edge by its unique Edge-ID. Throws an Exception if there is no Edge
     * with this ID.
     *
     * @param edgeId the Edge-ID
     * @return the Edge
     * @throws OpenemsException on error
     */
    default Edge getEdgeOrError(String edgeId) throws OpenemsException {
        Optional<Edge> edgeOpt = this.getEdge(edgeId);
        if (edgeOpt.isPresent()) {
            return edgeOpt.get();
        } else {
            throw new OpenemsException("Unable to get Edge for id [" + edgeId + "]");
        }
    }

    /**
     * Gets the User for the given User-ID.
     *
     * @param userId the User-ID
     * @return the User, or Empty
     */
    Optional<BackendUser> getUser(String userId);

    /**
     * Gets all Edges.
     *
     * @return collection of Edges.
     */
    Collection<Edge> getAllEdges();
}
