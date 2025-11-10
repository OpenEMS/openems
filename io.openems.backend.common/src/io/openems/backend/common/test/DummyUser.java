package io.openems.backend.common.test;

import static io.openems.common.session.Language.DEFAULT;

import java.util.NavigableMap;
import java.util.TreeMap;

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

	public static final DummyUser DUMMY_GUEST = new DummyUser(GUEST, GUEST, "", DEFAULT, Role.GUEST, new TreeMap<>(),
			false, JsonUtils.buildJsonObject() //
					.build());
	public static final DummyUser DUMMY_OWNER = new DummyUser(OWNER, OWNER, "", DEFAULT, Role.OWNER, new TreeMap<>(),
			false, JsonUtils.buildJsonObject() //
					.build());
	public static final DummyUser DUMMY_INSTALLER = new DummyUser(INSTALLER, INSTALLER, "", DEFAULT, Role.INSTALLER,
			new TreeMap<>(), false, JsonUtils.buildJsonObject() //
					.build());
	public static final DummyUser DUMMY_ADMIN = new DummyUser(ADMIN, ADMIN, "", DEFAULT, Role.ADMIN, new TreeMap<>(),
			false, JsonUtils.buildJsonObject() //
					.build());

	public DummyUser(String id, String name, String token, Language language, Role globalRole,
			NavigableMap<String, Role> roles, boolean hasMultipleEdges, JsonObject settings) {
		super(id, name, token, language, globalRole, roles, hasMultipleEdges, settings);
	}

}
