package io.openems.backend.metadata.odoo.odoo;

import static io.openems.backend.common.test.DummyUser.DUMMY_OWNER;
import static io.openems.backend.metadata.odoo.postgres.CredentialsTest.DUMMY_ODOO_CREDENTIALS;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.MetadataOdoo;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.DebugMode;
import io.openems.common.utils.ReflectionUtils;

class GetSetupProtocolTest {

	private static final int PROTOCOL_ID = 123;
	private static final int ODOO_EDGE_ID = 456;
	private static final String REPORT = "openems.report_openems_setup_protocol_template";
	private static final byte[] PDF = { 1, 2, 3 };

	private MetadataOdoo metadata;
	private MockedStatic<OdooUtils> odoo;

	@BeforeEach
	void setUp() {
		this.odoo = mockStatic(OdooUtils.class);
		this.odoo.when(() -> OdooUtils.getOdooReferenceId(any())).thenCallRealMethod();
		this.odoo.when(() -> OdooUtils.getOdooReport(DUMMY_ODOO_CREDENTIALS, REPORT, PROTOCOL_ID)).thenReturn(PDF);
		this.odoo
				.when(() -> OdooUtils.readOne(DUMMY_ODOO_CREDENTIALS, Field.SetupProtocol.ODOO_MODEL, PROTOCOL_ID,
						Field.SetupProtocol.EDGE))
				.thenReturn(Map.of(Field.SetupProtocol.EDGE.id(), new Object[] { ODOO_EDGE_ID, "edgeA" }));
		this.odoo.when(() -> OdooUtils.readOne(DUMMY_ODOO_CREDENTIALS, Field.EdgeDevice.ODOO_MODEL, ODOO_EDGE_ID,
				Field.EdgeDevice.NAME)).thenReturn(Map.of(Field.EdgeDevice.NAME.id(), "edgeB"));

		this.metadata = spy(new MetadataOdoo());
		doReturn(Role.OWNER).when(this.metadata).getUserRole(DUMMY_OWNER, "edgeA");
		doReturn(null).when(this.metadata).getUserRole(DUMMY_OWNER, "edgeB");
		final var config = new MyConfig(Protocol.HTTP, "127.0.0.1", 8069, 1, "admin", "admin", "127.0.0.1", 8068,
				"pgUser", "pgPassword", "db", 10, 10, 10, DebugMode.OFF, false, "");
		ReflectionUtils.setAttributeViaReflection(this.metadata, "odooHandler",
				new OdooHandler(this.metadata, null, config, null, null));
	}

	@AfterEach
	void tearDown() {
		this.odoo.close();
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "OWNER", "INSTALLER" })
	void downloadsProtocolForAuthorizedEdge(Role role) throws Exception {
		doReturn(role).when(this.metadata).getUserRole(DUMMY_OWNER, "edgeB");

		assertArrayEquals(PDF, this.metadata.getSetupProtocol(DUMMY_OWNER, PROTOCOL_ID));
		this.odoo.verify(() -> OdooUtils.getOdooReport(DUMMY_ODOO_CREDENTIALS, REPORT, PROTOCOL_ID));
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = { "OWNER", "INSTALLER" })
	void roleOnAnotherEdgeDoesNotAllowDownload(Role role) {
		doReturn(role).when(this.metadata).getUserRole(DUMMY_OWNER, "edgeA");

		this.assertDownloadDenied();
	}

	@Test
	void guestCannotDownloadProtocol() {
		doReturn(Role.GUEST).when(this.metadata).getUserRole(DUMMY_OWNER, "edgeB");

		this.assertDownloadDenied();
	}

	@ParameterizedTest
	@MethodSource("invalidReferences")
	void missingOrInvalidEdgeReferenceDoesNotAllowDownload(Object reference) {
		final var protocol = new HashMap<String, Object>();
		protocol.put(Field.SetupProtocol.EDGE.id(), reference);
		this.odoo.when(() -> OdooUtils.readOne(DUMMY_ODOO_CREDENTIALS, Field.SetupProtocol.ODOO_MODEL, PROTOCOL_ID,
				Field.SetupProtocol.EDGE)).thenReturn(protocol);

		this.assertDownloadDenied();
	}

	static Stream<Arguments> invalidReferences() {
		return Stream.of(null, false, new Object[] {}, new Object[] { "456", "edgeA" }, new Object[] { 0, "edgeA" },
				new Object[] { -1, "edgeA" }).map(reference -> Arguments.of(reference));
	}

	@Test
	void missingProtocolDoesNotAllowDownload() {
		this.odoo.when(() -> OdooUtils.readOne(DUMMY_ODOO_CREDENTIALS, Field.SetupProtocol.ODOO_MODEL, PROTOCOL_ID,
				Field.SetupProtocol.EDGE)).thenThrow(new OpenemsException("Protocol 123 not found"));

		this.assertDownloadDenied();
	}

	@Test
	void missingReferencedEdgeDoesNotAllowDownload() {
		this.odoo.when(() -> OdooUtils.readOne(DUMMY_ODOO_CREDENTIALS, Field.EdgeDevice.ODOO_MODEL, ODOO_EDGE_ID,
				Field.EdgeDevice.NAME)).thenThrow(new OpenemsException("Edge 456 not found"));

		this.assertDownloadDenied();
	}

	@ParameterizedTest
	@MethodSource("invalidNames")
	void invalidEdgeNameDoesNotAllowDownload(Object name) {
		final var edge = new HashMap<String, Object>();
		edge.put(Field.EdgeDevice.NAME.id(), name);
		this.odoo.when(() -> OdooUtils.readOne(DUMMY_ODOO_CREDENTIALS, Field.EdgeDevice.ODOO_MODEL, ODOO_EDGE_ID,
				Field.EdgeDevice.NAME)).thenReturn(edge);

		this.assertDownloadDenied();
	}

	static Stream<Object> invalidNames() {
		return Stream.of(null, false, "", " ");
	}

	private void assertDownloadDenied() {
		final var error = assertThrows(OpenemsNamedException.class,
				() -> this.metadata.getSetupProtocol(DUMMY_OWNER, PROTOCOL_ID));
		assertEquals("Access to setup protocol denied", error.getMessage());
		this.odoo.verify(() -> OdooUtils.getOdooReport(any(Credentials.class), anyString(), anyInt()), never());
	}
}
