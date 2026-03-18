package io.openems.backend.common.test;

import static io.openems.common.session.Language.DEFAULT;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.User;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public class DummyUser extends User {

	private static final String GUEST = "guest";
	private static final String OWNER = "owner";
	private static final String INSTALLER = "installer";
	private static final String ADMIN = "admin";

	public static final DummyUser DUMMY_GUEST = new DummyUser(GUEST, GUEST, "", DEFAULT, Role.GUEST, false,
			JsonUtils.buildJsonObject() //
					.build());
	public static final DummyUser DUMMY_OWNER = new DummyUser(OWNER, OWNER, "", DEFAULT, Role.OWNER, false,
			JsonUtils.buildJsonObject() //
					.build());
	public static final DummyUser DUMMY_INSTALLER = new DummyUser(INSTALLER, INSTALLER, "", DEFAULT, Role.INSTALLER,
			false, JsonUtils.buildJsonObject() //
					.build());
	public static final DummyUser DUMMY_ADMIN = new DummyUser(ADMIN, ADMIN, "", DEFAULT, Role.ADMIN, false,
			JsonUtils.buildJsonObject() //
					.build());

	public DummyUser(String id, String name, String token, Language language, Role globalRole, boolean hasMultipleEdges,
			JsonObject settings) {
		super(id, name, token, language, globalRole, hasMultipleEdges, settings);
	}

}
