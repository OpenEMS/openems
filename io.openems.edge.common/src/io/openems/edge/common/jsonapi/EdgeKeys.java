package io.openems.edge.common.jsonapi;

import io.openems.edge.common.user.User;

public final class EdgeKeys {

	public static final Key<User> USER_KEY = new Key<>("user", User.class);

	public static final Key<Boolean> IS_FROM_BACKEND_KEY = new Key<>("isFromBackend", Boolean.class);

	private EdgeKeys() {
	}

}
