package io.openems.backend.metadata.odoo;

import static io.openems.backend.common.test.DummyUser.DUMMY_INSTALLER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

class MetadataOdooSetupProtocolTest {

	@Test
	void allowsInstallerOnTargetEdge() throws Exception {
		var metadata = mock(MetadataOdoo.class, CALLS_REAL_METHODS);
		doReturn(Role.INSTALLER).when(metadata).getUserRole(DUMMY_INSTALLER, "edgeA");
		var protocol = JsonUtils.buildJsonObject() //
				.add(MetadataOdoo.ODOO_EDGE_NAME, JsonUtils.buildJsonObject() //
						.addProperty("id", "edgeA") //
						.build()) //
				.build();

		metadata.assertCanSubmitSetupProtocol(DUMMY_INSTALLER, protocol);
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "OWNER", "GUEST" })
	void rejectsInsufficientRoleOnTargetEdge(Role role) {
		var metadata = mock(MetadataOdoo.class, CALLS_REAL_METHODS);
		doReturn(role).when(metadata).getUserRole(DUMMY_INSTALLER, "edgeB");
		var protocol = JsonUtils.buildJsonObject() //
				.add(MetadataOdoo.ODOO_EDGE_NAME, JsonUtils.buildJsonObject() //
						.addProperty("id", "edgeB") //
						.build()) //
				.add("other", JsonUtils.buildJsonObject() //
						.addProperty("id", "edgeA") //
						.build()) //
				.build();

		var exception = assertThrows(OpenemsNamedException.class,
				() -> metadata.assertCanSubmitSetupProtocol(DUMMY_INSTALLER, protocol));
		assertEquals(OpenemsError.COMMON_ROLE_ACCESS_DENIED, exception.getError());
	}

	@Test
	void rejectsMissingRoleOnTargetEdge() {
		var metadata = mock(MetadataOdoo.class, CALLS_REAL_METHODS);
		doReturn(null).when(metadata).getUserRole(DUMMY_INSTALLER, "edgeB");
		var protocol = JsonUtils.buildJsonObject() //
				.add(MetadataOdoo.ODOO_EDGE_NAME, JsonUtils.buildJsonObject() //
						.addProperty("id", "edgeB") //
						.build()) //
				.build();

		var exception = assertThrows(OpenemsNamedException.class,
				() -> metadata.assertCanSubmitSetupProtocol(DUMMY_INSTALLER, protocol));
		assertEquals(OpenemsError.COMMON_ROLE_UNDEFINED, exception.getError());
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "null" })
	void rejectsMissingTargetEdge(String edgeJson) throws Exception {
		var metadata = mock(MetadataOdoo.class, CALLS_REAL_METHODS);
		var protocol = JsonUtils.buildJsonObject() //
				.add(MetadataOdoo.ODOO_EDGE_NAME, JsonUtils.parse(edgeJson)) //
				.build();

		assertThrows(OpenemsNamedException.class,
				() -> metadata.assertCanSubmitSetupProtocol(DUMMY_INSTALLER, protocol));
	}
}
