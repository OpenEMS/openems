package io.openems.backend.metadata.odoo.odoo;

import static io.openems.backend.metadata.odoo.postgres.CredentialsTest.DUMMY_ODOO_CREDENTIALS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.utils.JsonUtils;

class HttpBridgeOdooAuthenticationServiceTest {

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
	void testIsSessionExpired() {
		try (var service = new HttpBridgeOdooAuthenticationService(null, null)) {
			assertTrue(service.isSessionExpired(HttpResponse.ok(DUMMY_SESSION_EXPIRED_RESPONSE), null));
		}
	}

	@Test
	void testLoginAsAdmin() throws Exception {
		final var testBundle = DummyBridgeHttpBundle.of();

		final var bridge = testBundle.bridgeFactory().get();
		final var odooService = bridge
				.createService(new HttpBridgeOdooAuthenticationServiceDefinition(DUMMY_ODOO_CREDENTIALS));

		final var endpointCalled = testBundle.expect(endpoint -> {
			assertThat(endpoint.url(), endsWith("/web/session/authenticate"));
			assertEquals(JsonUtils.parseOptional("""
					{
					  "jsonrpc": "2.0",
					  "method": "call",
					  "params": {
					    "login": "%s",
					    "password": "%s",
					    "db": "%s"
					  }
					}
					""".formatted(DUMMY_ODOO_CREDENTIALS.login(), DUMMY_ODOO_CREDENTIALS.password(),
					DUMMY_ODOO_CREDENTIALS.database())), JsonUtils.parseOptional(endpoint.body()));
			return true;
		}).toBeCalled();

		final var token = "ansibeiaewf";
		testBundle.forceNextSuccessfulResult(HttpResponse.ok("""
				{
				   "jsonrpc": "2.0",
				   "id": null,
				   "result": {
				   }
				 }
				""")//
				.withHeader("set-cookie", List.of("session_id=" + token, "Expires=Thu, 08 Jul 2027 07:59:21 GMT",
						"Max-Age=604800", "HttpOnly", "Path=/")));

		final var result = odooService.loginAsAdmin().get();

		assertTrue(endpointCalled.get());
		assertEquals(token, result);
	}

}