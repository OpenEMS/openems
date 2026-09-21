package io.openems.backend.metadata.odoo.odoo;

import static io.openems.backend.common.test.DummyUser.DUMMY_OWNER;
import static io.openems.backend.metadata.odoo.postgres.CredentialsTest.DUMMY_ODOO_CREDENTIALS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.backend.metadata.odoo.odoo.http.OdooDeviceData;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.channel.Level;
import io.openems.common.jsonrpc.request.GetEdgesRequest;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

class HttpBridgeOdooServiceTest {

	private DummyBridgeHttpBundle testBundle;
	private HttpBridgeOdooService odooService;

	@BeforeEach
	void setUp() {
		this.testBundle = DummyBridgeHttpBundle.of();

		final var bridge = this.testBundle.bridgeFactory().get();
		this.odooService = bridge.createService(new HttpBridgeOdooServiceDefinition(DUMMY_ODOO_CREDENTIALS));
	}

	@Test
	void testGetEdges() throws Exception {
		final var endpointCalled = this.testBundle.expect(endpoint -> {
			assertThat(endpoint.url(), endsWith("/openems_backend/get_edges"));
			assertEquals(JsonUtils.parseOptional("""
					{
					  "params": {
					    "external_uid": "owner",
					    "page": 1,
					    "limit": 10,
					    "query": "edge"
					  }
					}
					"""), JsonUtils.parseOptional(endpoint.body()));
			return true;
		}).toBeCalled();

		this.testBundle.forceNextSuccessfulResult(HttpResponse.ok("""
				{
				  "jsonrpc": "2.0",
				  "id": null,
				  "result": {
				    "devices": [
				      {
				        "id": 39,
				        "name": "edge1",
				        "comment": "My first edge",
				        "producttype": "prototype",
				        "role": "admin",
				        "lastmessage": "2026-07-02 09:26:03",
				        "openems_sum_state_level": "fault"
				      },
				      {
				        "id": 44,
				        "name": "edge2",
				        "comment": "My second edge",
				        "producttype": "real-product",
				        "role": "installer",
				        "lastmessage": "2026-07-03 19:26:03",
				        "openems_sum_state_level": "ok"
				      }
				    ]
				  }
				}
				"""));

		final var result = this.odooService
				.getEdges(DUMMY_OWNER, new GetEdgesRequest.PaginationOptions(1, 10, "edge", null)).get();

		assertTrue(endpointCalled.get());

		assertEquals(List.of(//
				new OdooDeviceData("edge1", "My first edge", "prototype", Role.ADMIN,
						ZonedDateTime.parse("2026-07-02T09:26:03Z[UTC]"), Level.FAULT, null, null), //
				new OdooDeviceData("edge2", "My second edge", "real-product", Role.INSTALLER,
						ZonedDateTime.parse("2026-07-03T19:26:03Z[UTC]"), Level.OK, null, null) //
		), result.devices());
	}

}