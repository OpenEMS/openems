package io.openems.edge.common.jsonapi;

import io.openems.common.session.Role;
import io.openems.edge.common.user.User;

public final class EdgeGuards {

	/**
	 * Creates a {@link JsonrpcEndpointGuard} which checks if the {@link Role} of
	 * the current {@link User} is atleast the given {@link Role}.
	 * 
	 * @param role the role which the user should atleast have
	 * @return the created {@link JsonrpcEndpointGuard}
	 */
	public static JsonrpcEndpointGuard roleIsAtleast(Role role) {
		return new JsonrpcRoleEndpointGuard(role);
	}

	/**
	 * Creates a {@link JsonrpcEndpointGuard} which checks if the request is NOT
	 * from the backend and if so checks if the {@link Role} of the current
	 * {@link User} is atleast the given {@link Role}.
	 * 
	 * @param role the role which the user should atleast have
	 * @return the created {@link JsonrpcEndpointGuard}
	 */
	public static JsonrpcEndpointGuard roleIsAtleastNotFromBackend(Role role) {
		return new JsonrpcBackendRoleEndpointGuard(role, false);
	}

	/**
	 * Creates a {@link JsonrpcEndpointGuard} which checks if the request is from
	 * the backend and if so checks if the {@link Role} of the current {@link User}
	 * is atleast the given {@link Role}.
	 * 
	 * @param role the role which the user should atleast have
	 * @return the created {@link JsonrpcEndpointGuard}
	 */
	public static JsonrpcEndpointGuard roleIsAtleastFromBackend(Role role) {
		return new JsonrpcBackendRoleEndpointGuard(role, true);
	}

	private EdgeGuards() {
	}

}
