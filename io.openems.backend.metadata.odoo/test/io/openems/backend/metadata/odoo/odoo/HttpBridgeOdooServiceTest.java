package io.openems.backend.metadata.odoo.odoo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import io.openems.backend.metadata.odoo.odoo.http.OdooDeviceData;
import io.openems.backend.metadata.odoo.odoo.http.OdooGetEdgeWithRoleRequest;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.channel.Level;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public class HttpBridgeOdooServiceTest {

	private static final Credentials DUMMY_ODOO_CREDENTIALS = new Credentials("http://127.0.0.1:8069", 1, "admin",
			"db");
	private static final String DUMMY_SESSION_EXPIRED_RESPONSE = """
			{
			  "jsonrpc": "2.0",
			  "id": null,
			  "error": {
			    "code": 100,
			    "message": "Odoo Session Expired",
			    "data": {
			      "name": "odoo.http.SessionExpiredException",
			      "debug": "Traceback (most recent call last):\\n  File \\"/opt/odoo/server/odoo/http.py\\", line 2175, in _transactioning\\n    return service_model.retrying(func, env=self.env)\\n           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^\\n  File \\"/opt/odoo/server/odoo/service/model.py\\", line 156, in retrying\\n    result = func()\\n             ^^^^^^\\n  File \\"/opt/odoo/server/odoo/http.py\\", line 2140, in _serve_ir_http\\n    self.registry['ir.http']._authenticate(rule.endpoint)\\n  File \\"/opt/odoo/server/odoo/addons/base/models/ir_http.py\\", line 263, in _authenticate\\n    cls._authenticate_explicit(auth)\\n  File \\"/opt/odoo/server/odoo/addons/base/models/ir_http.py\\", line 272, in _authenticate_explicit\\n    getattr(cls, f'_auth_method_{auth}')()\\n  File \\"/opt/odoo/server/odoo/addons/base/models/ir_http.py\\", line 248, in _auth_method_user\\n    raise http.SessionExpiredException(\\"Session expired\\")\\nodoo.http.SessionExpiredException: Session expired\\n",
			      "message": "Session expired",
			      "arguments": [
			        "Session expired"
			      ],
			      "context": {}
			    }
			  }
			}
			""";

	@Test
	public void testAuthenticateOnFirstRequest() throws Exception {
		final var testBundle = new DummyBridgeHttpBundle();

		final var bridge = testBundle.factory().get();
		final var odooService = bridge.createService(new HttpBridgeOdooServiceDefinition(DUMMY_ODOO_CREDENTIALS));

		// 1. initially there is no session token available so it goes to authenticate
		// immediately
		testBundle.forceNextSuccessfulResult(HttpResponse.ok("{}") //
				.withHeader("Set-Cookie", List.of("session_id=123412")));

		// 2. Request is the edge query
		final var edge = new OdooDeviceData("edge0", "", "", Role.GUEST, null, Level.OK, null, null);
		testBundle.forceNextSuccessfulResult(HttpResponse.ok(JsonUtils.buildJsonObject() //
				.add("result", OdooDeviceData.serializer().serialize(edge)) //
				.build().toString()));

		final var result = odooService.getEdgeWithRole(new OdooGetEdgeWithRoleRequest("1111", "edge0")).get();
		assertEquals(edge, result);
	}

	@Test
	public void testReauthenticateOnSessionExpired() throws Exception {
		final var testBundle = new DummyBridgeHttpBundle();

		final var bridge = testBundle.factory().get();
		final var odooService = bridge.createService(new HttpBridgeOdooServiceDefinition(DUMMY_ODOO_CREDENTIALS));

		// 1. initially there is no session token available so it goes to authenticate
		// immediately
		testBundle.forceNextSuccessfulResult(HttpResponse.ok("{}") //
				.withHeader("Set-Cookie", List.of("session_id=123412")));

		// 2. Request is the edge query
		final var edge = new OdooDeviceData("edge0", "", "", Role.GUEST, null, Level.OK, null, null);
		testBundle.forceNextSuccessfulResult(HttpResponse.ok(JsonUtils.buildJsonObject() //
				.add("result", OdooDeviceData.serializer().serialize(edge)) //
				.build().toString()));

		var result = odooService.getEdgeWithRole(new OdooGetEdgeWithRoleRequest("1111", "edge0")).get();
		assertEquals(edge, result);

		testBundle.forceNextSuccessfulResult(HttpResponse.ok(DUMMY_SESSION_EXPIRED_RESPONSE));
		testBundle.forceNextSuccessfulResult(HttpResponse.ok(DUMMY_SESSION_EXPIRED_RESPONSE));

		assertThrows(ExecutionException.class, () -> {
			odooService.getEdgeWithRole(new OdooGetEdgeWithRoleRequest("1111", "edge0")).get();
		});

		testBundle.forceNextSuccessfulResult(HttpResponse.ok("{}") //
				.withHeader("Set-Cookie", List.of("session_id=123412")));
		testBundle.forceNextSuccessfulResult(HttpResponse.ok(JsonUtils.buildJsonObject() //
				.add("result", OdooDeviceData.serializer().serialize(edge)) //
				.build().toString()));
		result = odooService.getEdgeWithRole(new OdooGetEdgeWithRoleRequest("1111", "edge0")).get();
		assertEquals(edge, result);
	}

}